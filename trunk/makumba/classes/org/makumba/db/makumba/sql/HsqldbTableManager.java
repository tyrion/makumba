/*
 * Created on 20-sep-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package org.makumba.db.makumba.sql;

import org.makumba.FieldDefinition;

public class HsqldbTableManager extends org.makumba.db.makumba.sql.TableManager {

    @Override
    protected int getSQLType(String fieldName) {
        switch (getFieldDefinition(fieldName).getIntegerType()) {
            case FieldDefinition._text:
                return -4;
            default:
                return super.getSQLType(fieldName);
        }
    }

    @Override
    protected String getColumnAlterKeyword() {
        return "ALTER COLUMN";
    }

    @Override
    public String getFieldDBIndexName(String fieldName) {
        return getFieldDBName(fieldName) + "_" + this.getDBName();
    }

    @Override
    protected String getQueryAutoIncrementSyntax() {
        return "CALL IDENTITY()";
    }

    @Override
    protected String getCreateAutoIncrementSyntax() {
        // in HSQLDB IDENTITY columns are by default primary key
        return "GENERATED BY DEFAULT AS IDENTITY";
    }

    @Override
    public String in_boolean_Create(String fieldName, Database d) {
        return getFieldDBName(fieldName) + " " + getFieldDBType(fieldName, d);
    }

    @Override
    public String foreignKeyCreateSyntax(String fieldName, String fkTableName, String fkFieldName) {

        return "ALTER TABLE " + getDBName() + " ADD CONSTRAINT "
                + shortIndexName(((TableManager) getDatabase().getTable(fkTableName)).getDBName(), fieldName)
                + " FOREIGN KEY " + " (" + getFieldDBName(fieldName).toUpperCase() + ") REFERENCES "
                + ((TableManager) getDatabase().getTable(fkTableName)).getDBName().toUpperCase() + " ("
                + ((TableManager) getDatabase().getTable(fkTableName)).getFieldDBName(fkFieldName).toUpperCase() + ")";
    }

}
