// /////////////////////////////
//  Makumba, Makumba tag library
//  Copyright (C) 2000-2003 http://www.makumba.org
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
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

package org.makumba.commons.formatters;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Hashtable;

import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;

/**
 * Generic formatter of Makumba data. Depending on the data type, calls specific formatters an applies them for each
 * field of a record.
 * 
 * @author Cristian Bogdan
 * @author Rudolf Mayer
 * @version $Id$
 */

public class RecordFormatter implements Serializable {

    public DataDefinition dd;

    public String[] expr;

    protected transient FieldFormatter[] formatterArray;

    protected boolean isSearchForm;

    private Object formIdentifier;

    public RecordFormatter() {
    }

    private static final long serialVersionUID = 1L;

    public RecordFormatter(DataDefinition dd, Hashtable<String, String> names, boolean isSearchForm,
            Object formIdentifier) {
        this.dd = dd;
        this.isSearchForm = isSearchForm;
        this.formIdentifier = formIdentifier;
        initFormatters();

        expr = new String[dd.getFieldDefinitions().size()];

        int i = 0;
        for (FieldDefinition fd : dd.getFieldDefinitions()) {
            expr[i++] = names.get(fd.getName());
        }
    }

    protected String applyParameters(FieldFormatter ff, Dictionary<String, Object> formatParams, String s) {
        return s;
    }

    public String format(int i, Object value, Dictionary<String, Object> formatParams) {
        formatterArray[i].checkParams(this, i, formatParams);
        return applyParameters(formatterArray[i], formatParams, formatterArray[i].format(this, i, value, formatParams));
    }

    protected void initFormatters() {
        formatterArray = new FieldFormatter[dd.getFieldDefinitions().size()];
        int i = 0;
        for (FieldDefinition fd : dd.getFieldDefinitions()) {
            switch (fd.getIntegerType()) {
                case FieldDefinition._ptr:
                case FieldDefinition._ptrRel:
                case FieldDefinition._ptrOne:
                case FieldDefinition._ptrIndex:
                    formatterArray[i] = ptrFormatter.getInstance();
                    break;
                case FieldDefinition._intEnum:
                    formatterArray[i] = intEnumFormatter.getInstance();
                    break;
                case FieldDefinition._date:
                    formatterArray[i] = dateFormatter.getInstance();
                    break;
                case FieldDefinition._dateCreate:
                case FieldDefinition._dateModify:
                    formatterArray[i] = timestampFormatter.getInstance();
                    break;
                default:
                    formatterArray[i] = FieldFormatter.getInstance();
            }
            i++;
        }
    }

    public Object getFormIdentifier() {
        return formIdentifier;
    }
}