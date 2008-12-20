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

package org.makumba.forms.html;

import java.util.Dictionary;

import org.makumba.HtmlUtils;
import org.makumba.commons.StringUtils;
import org.makumba.commons.formatters.FieldFormatter;
import org.makumba.commons.formatters.InvalidValueException;
import org.makumba.commons.formatters.RecordFormatter;
import org.makumba.providers.Configuration;

public class charEditor extends FieldEditor {
	
	private static final class SingletonHolder {
		static final FieldEditor singleton = new charEditor();
	}

	/** Don't use this, use getInstance() */
	protected charEditor() {}

	public static FieldFormatter getInstance() {
		return SingletonHolder.singleton;
	}
	

//	public static final charEditor singleton = new charEditor();

	static String[] _params = { "default", "empty", "type", "size", "maxlength", "autoComplete" };

	static String[][] _paramValues = { null, null, { "text", "password" },
			null, null, new String[] { "true", "false" } };

	@Override
    public String[] getAcceptedParams() {
		return _params;
	}

	@Override
    public String[][] getAcceptedValue() {
		return _paramValues;
	}

    public int getWidth(RecordFormatter rf, int fieldIndex)
    { return  rf.dd.getFieldDefinition(fieldIndex).getWidth(); }
    
	public String getParams(RecordFormatter rf, int fieldIndex, Dictionary formatParams) {
		String ret = getIntParamString(rf, fieldIndex, formatParams, "size");
		int n = getIntParam(rf, fieldIndex, formatParams, "maxlength");
		if (n > getWidth(rf, fieldIndex)) {
            throw new InvalidValueException(rf.expr[fieldIndex],
					"invalid too big for maxlength " + n);
        }
		if (n == -1) {
            n = getWidth(rf, fieldIndex);
        }
		ret += "maxlength=\"" + n + "\" ";
		return ret;
	}

	/** Formats the input-field in case of null object */
	@Override
    public String formatNull(RecordFormatter rf, int fieldIndex, Dictionary formatParams) {
		return formatNotNull(rf, fieldIndex, null, formatParams);
	}

	/** Formats the input-field in case of not-null object */
	@Override
    public String formatNotNull(RecordFormatter rf, int fieldIndex, Object o, Dictionary formatParams) {
		boolean autoComplete = formatParams.get("autoComplete") != null && formatParams.get("autoComplete").equals("true");
	    String test = getParams(rf, fieldIndex, formatParams);
		String res = "", id="";
		
		res += "<input name=\"" + getInputName(rf, fieldIndex, formatParams) + "\" type=\""
				+ getInputType(rf, fieldIndex, formatParams) + "\" value=\""
				+ formatValue(rf, fieldIndex, o, formatParams) + "\" "
				+ test + getExtraFormatting(rf, fieldIndex, formatParams)
				+ ">";
		
		// the second part of the auto-complete, i.e. the dropdown that appears
		if(autoComplete) {
		    // getting the id won't work for dates and the other type commented in the hack in FieldFormatter
            id = StringUtils.getParam("id", getExtraFormatting(rf, fieldIndex, formatParams));
            
		    res += "<div id=\"autocomplete_choices_"+id+"\" class=\"autocomplete\"></div>";
		    
		    // TODO adjust the URL: figure a way to give the URL to the right servlet, using Configuration (probably tweak it) and then pass also the right params somehow
//            res += "<script type=\"text/javascript\">new Ajax.Autocompleter('"+id+"', 'autocomplete_choices_"+id+"', '"+"/tests"+Configuration.getMakumbaAutoCompleteLocation()+"?type="+rf.dd.getFieldDefinition(fieldIndex).getOriginalFieldDefinition().getDataDefinition().getName()+"&field="+rf.dd.getFieldDefinition(fieldIndex).getOriginalFieldDefinition().getName()+"', {"
//                + "minChars: 2, paramName: 'value'});</script>";
		    res += "<script type=\"text/javascript\">MakumbaAutoComplete.AutoComplete(\""+id+"\", \""+Configuration.getMakumbaAutoCompleteLocation()+"\", \""+ rf.dd.getFieldDefinition(fieldIndex).getOriginalFieldDefinition().getDataDefinition().getName()+"\", \""+rf.dd.getFieldDefinition(fieldIndex).getOriginalFieldDefinition().getName()+"\");</script>";
        
		}
		
		return res;
	}

	/** Formats the value to appear in an input statement. */
	@Override
    public String formatValue(RecordFormatter rf, int fieldIndex, Object o, Dictionary formatParams) {
		String s = (o == null) ? null : HtmlUtils.string2html(o.toString());
		return resetValueFormat(rf, fieldIndex, s, formatParams);
	}

	/*
	 * Formats the value to appear in hidden input statement: don't overload
	 * default behaviour set in FieldEditor.
	 */
	// public String formatHiddenValue(Object o, Dictionary formatParams) {}

	// public String getLiteral(Object o, Dictionary formatParams)
	// { }
	public String getInputType(RecordFormatter rf, int fieldIndex, Dictionary formatParams) {
		String s = (String) formatParams.get("type");
		if (s == null) {
            s = "text";
        }
		return s;
	}
}
