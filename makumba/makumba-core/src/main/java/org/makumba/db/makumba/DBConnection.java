///////////////////////////////
//  Makumba, Makumba tag library
//  Copyright (C) 2000-2003  http://www.makumba.org
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
//  -------------
//  $Id$
//  $Name$
/////////////////////////////////////

package org.makumba.db.makumba;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.makumba.DataDefinition;
import org.makumba.MakumbaError;
import org.makumba.Pointer;
import org.makumba.ProgrammerError;
import org.makumba.Transaction;
import org.makumba.db.TransactionImplementation;
import org.makumba.providers.DataDefinitionProvider;
import org.makumba.providers.TransactionProvider;

/**
 * This is the Makumba-specific implementation of a {@link Transaction}
 * 
 * @author Cristian Bogdan
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @version $Id$
 */
public abstract class DBConnection extends TransactionImplementation {

    protected String dataSource;

    protected org.makumba.db.makumba.Database db;

    protected DBConnection(TransactionProvider tp) {
        super(tp);
    }// for the wrapper

    public DBConnection(Database database, TransactionProvider tp) {
        this(tp);
        this.db = database;
        this.ddp = DataDefinitionProvider.getInstance();
    }

    public DBConnection(Database database, String dataSource, TransactionProvider tp) {
        this(database, tp);
        this.dataSource = dataSource;
    }

    public org.makumba.db.makumba.Database getHostDatabase() {
        return db;
    }

    /** Get the name of the database in the form host[_port]_dbprotocol_dbname */
    @Override
    public String getName() {
        return db.getName();
    }

    Map<String, Pointer> locks = new HashMap<String, Pointer>(13);

    Hashtable<String, Object> lockRecord = new Hashtable<String, Object>(5);

    @Override
    public void lock(String symbol) {
        lockRecord.clear();
        lockRecord.put("name", symbol);
        locks.put(symbol, insert("org.makumba.db.makumba.Lock", lockRecord));
    }

    @Override
    public void unlock(String symbol) {
        Pointer p = locks.get(symbol);
        if (p == null) {
            throw new ProgrammerError(symbol + " not locked in connection " + this);
        }
        deleteLock(symbol);
    }

    protected void deleteLock(String symbol) {
        locks.remove(symbol);
        // we need to delete after the lock name instead of the pointer
        // in order not to produce deadlock
        delete("org.makumba.db.makumba.Lock l", "l.name=$1", symbol);
    }

    protected void unlockAll() {
        for (String string : locks.keySet()) {
            deleteLock(string);
        }
    }

    @Override
    protected StringBuffer writeReadQuery(Pointer p, Enumeration<String> e) {
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT ");
        String separator = "";
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            DataDefinition r = ddp.getDataDefinition(p.getType());
            if (!(o instanceof String)) {
                throw new org.makumba.NoSuchFieldException(r,
                        "Dictionaries passed to makumba DB operations should have String keys. Key <" + o
                                + "> is of type " + o.getClass() + r.getName());
            }
            if (r.getFieldDefinition((String) o) == null) {
                throw new org.makumba.NoSuchFieldException(r, (String) o);
            }
            String s = (String) o;
            sb.append(separator).append("p.").append(s).append(" as ").append(s);
            separator = ",";
        }
        sb.append(" FROM " + p.getType() + " p WHERE p=$1");
        return sb;
    }

    @Override
    protected Vector<Dictionary<String, Object>> executeReadQuery(Pointer p, StringBuffer sb) {
        Object[] params = { p };
        Vector<Dictionary<String, Object>> v = executeQuery(sb.toString(), params);
        return v;
    }

    /** insert a record */
    @Override
    public Pointer insert(String type, Dictionary<String, Object> data) {
        Table t = db.getTable(type);
        t.computeInsertHook();

        if (t.insertHook != null) {
            Hashtable<String, Object> h = new Hashtable<String, Object>();
            for (Enumeration<String> e = data.keys(); e.hasMoreElements();) {
                String k = e.nextElement();
                h.put(k, data.get(k));
            }
            data = h;
        }

        if (t.insertHook == null || t.insertHook.transform(data, this)) {
            return super.insert(type, data);
        }
        return null;
    }

    /** mass insert of a record **/
    @Override
    public Vector<Pointer> insert(String type, Collection<Dictionary<String, Object>> data) {
        throw new MakumbaError("Not implemented");
    }

    /**
     * Execute a parametrized OQL query.
     * 
     * @return a Vector of Dictionaries
     */
    @Override
    public Vector<Dictionary<String, Object>> executeQuery(String OQL, Object args, int offset, int limit) {
        return getQuery(OQL, null).execute(paramsToMap(args), this, offset, limit);
    }

    @Override
    protected int insertFromQueryImpl(String type, String OQL, Object args) {
        return getQuery(OQL, type).insert(paramsToMap(args), this);
    }

    private Query getQuery(String OQL, String type) {
        // Object[] k = { OQL, type };
        // return ((Query) getHostDatabase().queries.getResource(k))
        return new org.makumba.db.makumba.sql.Query((org.makumba.db.makumba.sql.Database) getHostDatabase(), OQL, type);
    }

    @Override
    public Vector<Dictionary<String, Object>> executeQuery(String OQL, Object args) {
        return executeQuery(OQL, args, 0, -1);
    }

    /**
     * Execute a parametrized update or delete. A null set means "delete"
     * 
     * @return a Vector of Dictionaries
     */
    @Override
    public int executeUpdate(String type, String set, String where, Object args) {
        if (set != null && set.trim().length() == 0) {
            throw new org.makumba.OQLParseError("Invalid empty update 'set' section in: UPDATE " + type
                    + " SET (empty!) WHERE " + where);
        }

        if (where != null && where.trim().length() == 0) {
            where = null;
        }

        Object[] multi = { type, set, where };

        return ((Update) getHostDatabase().updates.getResource(multi)).execute(this, paramsToMap(args));
    }

    public Query getQuery(String OQL) {
        return getQuery(OQL, null);
    }

    @Override
    public String getNullConstant() {
        return "nil";
    }

    @Override
    public String getDataSource() {
        return this.dataSource;
    }

    // FIXME should be done at construction time, but due to nature of how DB is now it's not possible
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    protected Object[] treatParam(Object args) {
        if (args == null) {
            return new Object[] {};
        } else if (args instanceof Vector) {
            Vector<?> v = (Vector<?>) args;
            Object[] param = new Object[v.size()];
            v.copyInto(param);
            return param;
        } else if (args instanceof Object[]) {
            return (Object[]) args;
        } else {
            Object p[] = { args };
            return p;
        }
    }
}