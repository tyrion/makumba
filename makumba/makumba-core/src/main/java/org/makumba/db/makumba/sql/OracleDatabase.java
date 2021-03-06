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

package org.makumba.db.makumba.sql;

import java.util.Properties;

import org.makumba.FieldDefinition;

/** the database adapter for PostgreSQL */
public class OracleDatabase extends org.makumba.db.makumba.sql.Database {
    /** simply calls super */
    public OracleDatabase(Properties p) {
        super(p);
    }

    protected int getMaxTableNameLength() {
        return 30;
    }

    protected int getMaxFieldNameLength() {
        return 30;
    }

    // TODO now in sqlEngines.properties -->OK?
    // /** returns oracle RecordManager */
    // protected Class getTableClass()
    // { return org.makumba.db.sql.oracle.RecordManager.class; }

    @Override
    protected String getJdbcUrl(Properties p) {
        String host = p.getProperty("#host");
        if (host.indexOf(':') < 0) {
            host = host + ":1521"; // define default port (must be specified)
        }
        return "jdbc:oracle:thin:@//" + host + "/" + p.getProperty("#database");
    }

    // moved from oracle.charManager
    /** returns char */
    @Override
    protected String get_char_FieldDBType(FieldDefinition fd) {
        switch (fd.getIntegerType()) {
            case FieldDefinition._char:
            case FieldDefinition._charEnum:
                return "VARCHAR2";
            default:
                return super.get_char_FieldDBType(fd);
        }
    }

}
