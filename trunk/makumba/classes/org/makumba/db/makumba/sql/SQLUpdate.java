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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.makumba.DBError;
import org.makumba.InvalidValueException;
import org.makumba.MakumbaError;
import org.makumba.NotUniqueException;
import org.makumba.OQLParseError;
import org.makumba.db.makumba.Update;
import org.makumba.providers.ParameterTransformer;
import org.makumba.providers.QueryAnalysis;
import org.makumba.providers.QueryAnalysisProvider;
import org.makumba.providers.QueryProvider;
import org.makumba.providers.query.mql.MqlParameterTransformer;
import org.makumba.providers.query.mql.MqlQueryAnalysis;

public class SQLUpdate implements Update {

    ParameterAssigner assigner;

    String debugString;

    String updateCommand;

    String type;

    String setWhere;

    String DELIM;

    Database db;

    ParameterTransformer qG;

    QueryAnalysisProvider qP = QueryProvider.getQueryAnalzyer("oql");

    SQLUpdate(Database db, String from, String setWhere, String DELIM) {
        this.type = from;
        this.db = db;
        this.setWhere = setWhere;
        this.DELIM = DELIM;
    }

    private void compileUpdateCommand(Database db, String from, String setWhere, String DELIM, Map<String, Object> args)
            throws OQLParseError, MakumbaError {
        int whereMark = setWhere.indexOf(DELIM);
        String set = setWhere.substring(0, whereMark);
        String where = setWhere.substring(whereMark + DELIM.length());
        debugString = (set == null ? "delete" : "update") + " on type: <" + from + ">"
                + (set == null ? " " : " setting: <" + set + ">") + " where: <" + where + ">";

        if (set.trim().length() == 0) {
            set = null;
        }

        if (where.trim().length() == 0) {
            where = null;
        }

        // a primitive check, better one needs to be done after OQLAnalyzer's job
        if (from != null && from.indexOf(',') >= 0) {
            throw new org.makumba.OQLParseError("Only 1 table can be involved in " + debugString);
        }

        // make sure whitespace only consists of spaces
        from = from.replace('\t', ' ');

        // we determine the dummy label used in the arguments
        String label;
        try {
            label = from.substring(from.trim().indexOf(' ') + 1).trim();
        } catch (StringIndexOutOfBoundsException e) {
            throw new org.makumba.OQLParseError("Invalid delete/update 'type' section: " + from);
        }

        // to get the right SQL, we compile an imaginary MQL command made as follows:
        String OQLQuery = "SELECT " + (set == null ? label : set) + " FROM " + from;
        if (where != null) {
            OQLQuery += " WHERE " + where;
        }

        QueryAnalysis qA = qP.getQueryAnalysis(OQLQuery);
        qG = MqlParameterTransformer.getSQLQueryGenerator((MqlQueryAnalysis) qA, args);

        try {
            // FIXME: we should make sure here that the tree contains one single type!
            assigner = new ParameterAssigner(db, qA, qG);
        } catch (OQLParseError e) {
            throw new org.makumba.OQLParseError(e.getMessage() + "\r\nin " + debugString + "\n" + OQLQuery, e);
        }

        String fakeCommand;
        String fakeCommandUpper;
        try {
            fakeCommand = qG.getTransformedQuery(db.getNameResolverHook());
        } catch (RuntimeException e) {
            throw new MakumbaError(e, debugString + "\n" + OQLQuery);
        }
        fakeCommandUpper = fakeCommand.toUpperCase();
        StringBuffer replaceLabel = new StringBuffer();

        // we remove all "label." sequences from the SELECT part of the command
        int n = 0;
        int lastN;
        int maxN = fakeCommandUpper.indexOf(" FROM ");
        while (true) {
            lastN = n;
            n = fakeCommand.indexOf(label + ".", lastN);
            if (n == -1 || n > maxN) {
                replaceLabel.append(fakeCommand.substring(lastN, maxN));
                break;
            }
            replaceLabel.append(fakeCommand.substring(lastN, n));
            n += label.length() + 1;
        }

        // we remove the last instance of " label" from the FROM part of command
        lastN = fakeCommandUpper.indexOf(" WHERE ");
        if (lastN < 0) {
            lastN = fakeCommand.length();
        }
        n = fakeCommand.lastIndexOf(" " + label, lastN);
        replaceLabel.append(fakeCommand.substring(maxN, n));

        // we remove all "label." sequences from the WHERE part of the command
        n = lastN; // start where we left off above
        while (true) {
            lastN = n;
            n = fakeCommand.indexOf(label + ".", lastN);
            if (n == -1) {
                replaceLabel.append(fakeCommand.substring(lastN));
                break;
            }
            replaceLabel.append(fakeCommand.substring(lastN, n));
            n += label.length() + 1;
        }

        fakeCommand = replaceLabel.toString();
        fakeCommandUpper = fakeCommand.toUpperCase();

        // now we break the query SQL in pieces to form the update SQL
        StringBuffer command = new StringBuffer();
        command.append(set == null ? "DELETE FROM" : "UPDATE");
        command.append(fakeCommand.substring(fakeCommandUpper.indexOf(" FROM ") + 5,
            fakeCommandUpper.indexOf(" WHERE ")));
        if (set != null) {
            String setString = fakeCommand.substring(fakeCommandUpper.indexOf("SELECT ") + 7,
                fakeCommandUpper.indexOf(" FROM "));
            n = 0;
            while (true) {
                n = setString.toLowerCase().indexOf("is null", n);
                if (n == -1) {
                    n = setString.toLowerCase().indexOf("is  null", n);
                    if (n == -1) {
                        break;
                    }
                    setString = setString.substring(0, n) + " = null" + setString.substring(n + 8);
                    continue;
                }
                setString = setString.substring(0, n) + " = null" + setString.substring(n + 7);
            }
            command.append(" SET ").append(setString);
        }
        if (where != null) {
            command.append(fakeCommand.substring(fakeCommandUpper.indexOf(" WHERE ")));
        }

        debugString += "\n generated SQL: " + command;
        updateCommand = command.toString();
    }

    public int execute(org.makumba.db.makumba.DBConnection dbc, Map<String, Object> args) {

        compileUpdateCommand(db, type, setWhere, DELIM, args);

        PreparedStatement ps = ((SQLDBConnection) dbc).getPreparedStatement(updateCommand);
        try {
            String s = assigner.assignParameters(ps, qG.toParameterArray(args));
            if (s != null) {
                throw new InvalidValueException("Errors while trying to assign arguments to update:\n" + debugString
                        + "\n" + s);
            }

            // org.makumba.db.sql.Database db=(org.makumba.db.sql.Database)dbc.getHostDatabase();

            java.util.logging.Logger.getLogger("org.makumba.db.update.execution").fine(
                "" + db.getWrappedStatementToString(ps));
            java.util.Date d = new java.util.Date();
            int rez;
            try {
                rez = ps.executeUpdate();
            } catch (SQLException se) {
                Database db = (Database) dbc.getHostDatabase();
                if (db.isForeignKeyViolationException(se)) {
                    throw new org.makumba.ForeignKeyError(db.parseReadableForeignKeyErrorMessage(se));
                } else if (db.isDuplicateException(se)) {

                    NotUniqueException nue = new NotUniqueException(se.getMessage());
                    nue.setFields(db.getDuplicateFields(se));
                    throw nue;

                }
                org.makumba.db.makumba.sql.Database.logException(se);
                throw new DBError(se, debugString);
            }
            long diff = new java.util.Date().getTime() - d.getTime();
            java.util.logging.Logger.getLogger("org.makumba.db.update.performance").fine(
                "" + diff + " ms " + debugString);
            return rez;
        } catch (SQLException e) {
            throw new org.makumba.DBError(e);
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
                throw new org.makumba.DBError(e);
            }
        }
    }
}
