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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.makumba.DBError;
import org.makumba.DataDefinition;
import org.makumba.DataTransformer;
import org.makumba.FieldDefinition;
import org.makumba.Pointer;
import org.makumba.commons.NameResolver;
import org.makumba.db.DataHolder;

/**
 * This is a generic database table RecordHandler. TODO Document the methods in here
 * 
 * @see org.makumba.db.makumba.Database#getTable(org.makumba.abstr.RecordInfo)
 * @author Cristian Bogdan
 * @author Thomas Laroche
 * @author Marius Andra
 * @author Manuel Bernhardt <manuel@makumba.org>
 */
public abstract class Table // extends RecordHandler
{
    protected DataDefinition dd;

    public Table() {
    }

    org.makumba.db.makumba.Database db;

    /** What database does this table belong to */
    public org.makumba.db.makumba.Database getDatabase() {
        return db;
    }

    protected void setDataDefinition(DataDefinition dd) {
        this.dd = dd; // needed as we don't extend FieldHandler anymore
        for (FieldDefinition fd : dd.getFieldDefinitions()) {
            if (fd.getType().equals("ptr") || fd.getType().equals("ptrRel")) {
                // foreign
                relatedTables.put(fd.getName(), fd.getForeignTable());
            } else if (fd.getType().startsWith("ptr") && !fd.getType().equals("ptrIndex")
                    || fd.getType().startsWith("set")) {
                // subtable
                relatedTables.put(fd.getName(), fd.getSubtable());
            }
        }
    }

    public DataDefinition getDataDefinition() {
        return dd;
    }

    Hashtable<String, DataDefinition> relatedTables = new Hashtable<String, DataDefinition>();

    public FieldDefinition getFieldDefinition(String fieldName) {
        return dd.getFieldDefinition(fieldName);
    }

    /** get the related table for the field indicated by name (of any set or ptr type) */
    public Table getRelatedTable(String field) {
        return getDatabase().getTable(relatedTables.get(field));
        // return (Table)relatedTables.get(field);
    }

    /** does the table exist in the database ? */
    public abstract boolean canAdmin();

    /** does the table exist in the database ? */
    public abstract boolean exists();

    /** delete all the records created within the indicated database and return their number */
    public abstract int deleteFrom(DBConnection here, DBConnection source, boolean ignoreDbsv);

    /** does the field exist in the database ? */
    public abstract boolean exists(String fieldName);

    String selectAllWithDbsv;

    Object[] selectLimits = null;

    static final int BAR = 75;

    /** copies all records from the table1 to table2 */
    /**
     * TODO: makumba now supports query limitation (limit and offset). so this method (copyFrom) could run a number of
     * queries per table (instead of one query per table), limited to say 100 records each so we don't have memory
     * problems when the db is very big
     */
    void copyFrom(DBConnection dest, Table source, DBConnection sourceDB, boolean ignoreDbsv) {
        final String nm = getDataDefinition().getName();
        if (!source.exists() || nm.equals("org.makumba.db.makumba.Catalog")) {
            // catalog is never copied
            return;
        }

        if (selectAllWithDbsv == null) {
            StringBuffer list = new StringBuffer();
            String comma = "";

            for (FieldDefinition fd : dd.getFieldDefinitions()) {
                if (fd.getType().startsWith("set")) {
                    continue;
                }
                list.append(comma);
                comma = ", ";
                list.append("t.").append(fd.getName());
            }
            String indexName = getDataDefinition().getIndexPointerFieldName();
            String dbsvLimitation = "";
            if (!ignoreDbsv) {
                dbsvLimitation = "WHERE t." + indexName + ">=$1 AND t." + indexName + " <=$2";

                selectLimits = new Object[2];
                final int dbsv = sourceDB.getHostDatabase().getDbsv();
                selectLimits[0] = new Pointer() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getType() {
                        return nm;
                    }

                    @Override
                    public long longValue() {
                        return dbsv << MASK_ORDER;
                    }
                };
                selectLimits[1] = new Pointer() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getType() {
                        return nm;
                    }

                    @Override
                    public long longValue() {
                        return (dbsv + 1 << MASK_ORDER) - 1;
                    }
                };
            }
            selectAllWithDbsv = "SELECT " + list + " FROM " + nm + " t " + dbsvLimitation;
        }

        Vector<Dictionary<String, Object>> v = sourceDB.executeQuery(selectAllWithDbsv, selectLimits);
        if (v.size() == 0) {
            java.util.logging.Logger.getLogger("org.makumba.db.admin.copy").info(nm + ": no records to copy");
            return;
        }

        java.util.logging.Logger.getLogger("org.makumba.db.admin.copy").info(
            nm + ": starting copying " + v.size() + " records");

        System.out.print("|");
        for (int b = 0; b < BAR; b++) {
            System.out.print("-");
        }
        System.out.print("|\n ");
        System.out.flush();
        float step = (float) v.size() / BAR;

        int stars = 0;
        Hashtable<String, Object> data = new Hashtable<String, Object>(23);
        Hashtable<Object, String> nameKey = new Hashtable<Object, String>(23);

        int f = 0;
        for (FieldDefinition fd : dd.getFieldDefinitions()) {
            if (fd.getType().startsWith("set")) {
                continue;
            }
            nameKey.put("col" + (f + 1), fd.getName());
            f++;
        }

        for (int j = 0; j < v.size(); j++) {
            Dictionary<String, Object> d = v.elementAt(j);
            for (Enumeration<String> e = d.keys(); e.hasMoreElements();) {
                Object k = e.nextElement();
                data.put(nameKey.get(k), d.get(k));
            }

            dest.insert(getDataDefinition().getName(), data);

            // free up some memory
            data.clear();
            v.setElementAt(null, j);

            // display progress bar
            int nstars = (int) (((float) j + 1) / step);
            while (nstars > stars) {
                System.out.print("*");
                System.out.flush();
                stars++;
            }
        }
        System.out.println();
    }

    /**
     * Prepares everything needed for database management. identifies the database adapter that will be used, the type
     * of connection manager, etc. Might call create. Looks if secondary tables (from a one-to-many, sets) need to be
     * opened or created. Looks if the opened database actually respects the org.makumba file (if not, provides
     * functionality to convert the database to the new format). Toto: Your brain is a mess where your stupidity is
     * swimming.
     */
    protected abstract void open(Properties p, NameResolver nr);

    DataTransformer insertHook;

    void computeInsertHook() {
        if (insertHook == null) {
            String s = getDatabase().getConfiguration("insert#" + getDataDefinition().getName());
            if (s != null) {
                try {
                    insertHook = (DataTransformer) Class.forName(s).newInstance();
                } catch (Exception e) {
                    throw new DBError(e);
                }
            }
        }
    }

    public abstract void close();

    /** insert a record, return the pointer to it */
    public Pointer insertRecord(DBConnection c, Dictionary<String, Object> d) {
        return insertRecordImpl(c, d);
    }

    public abstract Pointer insertRecordImpl(DBConnection c, Dictionary<String, Object> d);

    public abstract void checkInsert(Dictionary<String, Object> fieldsToCheck,
            Dictionary<String, DataHolder> fieldsToIgnore, Dictionary<String, Object> allFields);

    public abstract void checkUpdate(Pointer pointer, Dictionary<String, Object> allFields);

    /** finds duplicates in the database, given a dictionary of data to insert/update **/
    public abstract void findDuplicates(DBConnection c, Dictionary<String, Object> d);
}
