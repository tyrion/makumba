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
//  $Id: FieldInfo.java 1538 2007-09-14 11:11:39Z manuel_gay $
//  $Name$
/////////////////////////////////////

//TODO extra comments about changes from refactoring

package org.makumba.providers.datadefinition.makumba;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.makumba.CompositeValidationException;
import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;
import org.makumba.InvalidValueException;
import org.makumba.Pointer;
import org.makumba.Text;
import org.makumba.ValidationRule;
import org.makumba.commons.StringUtils;
import org.makumba.providers.DataDefinitionProvider;

/**
 * This is a structure containing the elementary data about a field: name, type, attributes, description, and other
 * type-specific extra info. All this information is available through the associated <a
 * href=org.makumba.abstr.FieldHandler.html#_top_>FieldHandler </a>
 */
public class FieldInfo implements java.io.Serializable, FieldDefinition {

    private static final long serialVersionUID = 1L;

    DataDefinition dd;

    private String originalFieldDefinitionParent;

    private String originalFieldDefinitionName;

    static final DualHashBidiMap integerTypeMap = new DualHashBidiMap();

    public DataDefinition getDataDefinition() {
        return dd;
    }

    public FieldDefinition getOriginalFieldDefinition() {
        // we can't store a reference to the original field definition, otherwise it will be serialised in the form
        // responder, and in turn will serialise it's data definition, which might cause issues like locking..
        // thus, we do a lookup here
        if (originalFieldDefinitionParent == null) {
            return null;
        }
        DataDefinition dataDefinition = DataDefinitionProvider.getInstance().getDataDefinition(
            originalFieldDefinitionParent);
        return dataDefinition != null ? dataDefinition.getFieldDefinition(originalFieldDefinitionName) : null;
    }

    // TODO adapt setIntEnum and setCharEnum in FieldDefinition
    public static FieldInfo getFieldInfo(String name, Object type, boolean typeSearch) {
        if (type instanceof FieldInfo) {
            return new FieldInfo(name, (FieldInfo) type);
        }
        String t = ((String) type).trim();

        if (!typeSearch || t.indexOf(" ") == -1) {
            return new FieldInfo(name, t);
        }

        t = name + "=" + t;

        return (FieldInfo) new RecordParser().parse(t).getFieldDefinition(name);
    }

    public static FieldInfo getFieldInfo(String name, Object type, boolean typeSearch, String description) {
        FieldInfo a = getFieldInfo(name, type, typeSearch);
        a.description = description;

        return a;
    }

    public FieldInfo(DataDefinition ri, String name) {
        this.dd = ri;
        this.name = name;
    }

    public FieldInfo(FieldInfo fi) {
        this(fi.dd, fi.name);
        type = fi.type;
        fixed = fi.fixed;
        notNull = fi.notNull;
        notEmpty = fi.notEmpty;
        unique = fi.unique;
        defaultValue = fi.defaultValue;
        description = fi.description;
    }

    /** for temporary field info */
    public FieldInfo(String name, FieldInfo fi) {
        this.name = name;
        type = fi.type;
        fixed = fi.fixed;
        notEmpty = fi.notEmpty;
        unique = fi.unique;
        defaultValue = fi.defaultValue;
        description = fi.description;
        extra1 = fi.extra1;
        extra2 = fi.extra2;
        extra3 = fi.extra3;
        if (type.equals("ptrIndex")) {
            type = "ptr";
            extra1 = fi.getDataDefinition();
        }
        validationRules = fi.validationRules;

        // store names of original field definition and data definition; see getOriginalFieldDefinition() for details
        if (fi.getDataDefinition() != null) {
            originalFieldDefinitionParent = fi.getDataDefinition().getName();
            originalFieldDefinitionName = fi.getName();
        }
    }

    public FieldInfo(String name, String t) {
        try {
            this.name = name;
            this.type = t;
            fixed = false;
            notNull = false;
            notEmpty = false;
            unique = false;
            if (type.equals("char")) {
                extra2 = new Integer(255);
            } else if (type.startsWith("char")) {
                int n = type.indexOf("[");
                int m = type.indexOf("]");
                if (!type.endsWith("]") || type.substring(3, n).trim().length() > 1) {
                    throw new InvalidValueException("invalid char type " + type);
                }

                extra2 = new Integer(Integer.parseInt(type.substring(n + 1, m)));
                type = "char";
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new InvalidValueException("bad type " + type);
        } catch (NumberFormatException f) {
            throw new InvalidValueException("bad char[] size " + type);
        }
    }

    static {
        integerTypeMap.put("ptr", new Integer(FieldDefinition._ptr));
        integerTypeMap.put("ptrRel", new Integer(FieldDefinition._ptrRel));
        integerTypeMap.put("ptrOne", new Integer(FieldDefinition._ptrOne));
        integerTypeMap.put("ptrIndex", new Integer(FieldDefinition._ptrIndex));
        integerTypeMap.put("int", new Integer(FieldDefinition._int));
        integerTypeMap.put("intEnum", new Integer(FieldDefinition._intEnum));
        integerTypeMap.put("char", new Integer(FieldDefinition._char));
        integerTypeMap.put("charEnum", new Integer(FieldDefinition._charEnum));
        integerTypeMap.put("text", new Integer(FieldDefinition._text));
        integerTypeMap.put("binary", new Integer(FieldDefinition._binary));
        integerTypeMap.put("boolean", new Integer(FieldDefinition._boolean));
        integerTypeMap.put("date", new Integer(FieldDefinition._date));
        integerTypeMap.put("dateCreate", new Integer(FieldDefinition._dateCreate));
        integerTypeMap.put("dateModify", new Integer(FieldDefinition._dateModify));
        integerTypeMap.put("file", new Integer(FieldDefinition._file));
        integerTypeMap.put("set", new Integer(FieldDefinition._set));
        integerTypeMap.put("setComplex", new Integer(FieldDefinition._setComplex));
        integerTypeMap.put("nil", new Integer(FieldDefinition._nil));
        integerTypeMap.put("real", new Integer(FieldDefinition._real));
        integerTypeMap.put("setcharEnum", new Integer(FieldDefinition._setCharEnum));
        integerTypeMap.put("setintEnum", new Integer(FieldDefinition._setIntEnum));
    }

    public static String getStringType(int integerType) {
        return (String) integerTypeMap.getKey(new Integer(integerType));
    }

    public boolean isAssignableFrom(FieldDefinition fi) {
        switch (getIntegerType()) {
            case FieldDefinition._int:
                return is_int_AssignableFrom(fi);
            case FieldDefinition._intEnum:
                return is_intEnum_AssignableFrom(fi);
            case FieldDefinition._ptr:
            case FieldDefinition._ptrRel:
                return is_ptrRel_AssignableFrom(fi);
            case FieldDefinition._real:
                return is_real_AssignableFrom(fi);
            case FieldDefinition._set:
                return is_set_AssignableFrom(fi);
            default:
                return base_isAssignableFrom(fi);
        }
    }

    // Original from FieldHandler
    public boolean base_isAssignableFrom(FieldDefinition fi) {
        return fi.getType().equals("nil") || getType().equals(fi.getType());
    }

    // moved from intHandler
    public boolean is_int_AssignableFrom(FieldDefinition fi) {
        return base_isAssignableFrom(fi) || fi.getType().equals("intEnum");
    }

    // moved from IntEnumHandler
    public boolean is_intEnum_AssignableFrom(FieldDefinition fi) {
        return is_int_AssignableFrom(fi) || fi.getType().equals("int") || fi.getType().equals("char");
    }

    // moved from ptrRelHandler
    public boolean is_ptrRel_AssignableFrom(FieldDefinition fi) {
        return "nil".equals(fi.getType()) || getType().equals(fi.getType())
                && ((FieldInfo) fi).extra1 instanceof DataDefinition
                && ((DataDefinition) ((FieldInfo) fi).extra1).getName().equals(getForeignTable().getName());
    }

    // moved from realHandler
    public boolean is_real_AssignableFrom(FieldDefinition fi) {
        return base_isAssignableFrom(fi) || fi.getType().equals("intEnum") || fi.getType().equals("int");
    }

    // moved from setHandler
    public boolean is_set_AssignableFrom(FieldDefinition fi) {
        return "nil".equals(fi.getType()) || getType().equals(fi.getType())
                && getForeignTable().getName().equals(fi.getForeignTable().getName());
    }

    @Override
    public String toString() {
        return getType();
    }

    String name;

    String type;

    boolean fixed;

    boolean notNull;

    boolean notEmpty;

    boolean unique;

    Object defaultValue;

    String description;

    // those fields are only used by some types
    // FIXME: make specific, typed fields instead of those generic ones, which are really hard to understand!
    Object extra1, extra2, extra3;

    private Hashtable<String, ValidationRule> validationRules = new Hashtable<String, ValidationRule>();

    /** check if the value can be assigned */
    public Object checkValue(Object value) {
        switch (getIntegerType()) {
            case FieldDefinition._setIntEnum:
                return check_setintEnum_Value(value);
            default:
                return base_checkValue(value);
        }
    }

    // Original from FieldHandler
    /** check if the value can be assigned */
    public Object base_checkValue(Object value) {
        if (!value.equals(getNull())) {
            return checkValueImpl(value);
        }
        return value;
    }

    // moved from setintEnumHandler
    public Object check_setintEnum_Value(Object value) {
        try {
            // may be just an Integer
            Object o = getEnum().checkValue(value);
            Vector<Object> v = new Vector<Object>();
            if (o != null && o instanceof Integer) {
                v.addElement(o);
            }
            return v;
        } catch (org.makumba.InvalidValueException ive) {
        }

        normalCheck(value);
        Vector v = (Vector) value;

        for (int i = 0; i < v.size(); i++) {
            if (v.elementAt(i) == null || v.elementAt(i).equals(org.makumba.Pointer.NullInteger)) {
                throw new org.makumba.InvalidValueException(this, "set members cannot be null");
            }
            v.setElementAt(getEnum().checkValue(v.elementAt(i)), i);
        }
        return v;
    }

    /** check if the value can be assigned */
    public void checkInsert(Dictionary<String, Object> d) {
        Object o = d.get(getName());
        if (isNotNull() && (o == null || o.equals(getNull()))) {
            // FIXME: call this in RecordEditor.readFrom, to have more possible exceptions gathered at once
            throw new CompositeValidationException(new InvalidValueException(this, ERROR_NOT_NULL));
        } else if (isNotEmpty() && StringUtils.isEmpty(o)) {
            // FIXME: call this in RecordEditor.readFrom, to have more possible exceptions gathered at once
            throw new CompositeValidationException(new InvalidValueException(this, ERROR_NOT_EMPTY));
        }
        if (o != null) {
            d.put(getName(), checkValue(o));
        }
    }

    /**
     * Get deprecated values of the enumerator, works only for intEnum type.
     * 
     * @return <code>Vector</code>, or <code>null</code> if called on other types
     */
    public Vector<String> getDeprecatedValues() {
        switch (getIntegerType()) {
            case FieldDefinition._intEnum:
                return get_intEnum_DeprecatedValues();
            default:
                return null;
        }
    }

    // moved from intEnumHandler
    public Vector<String> get_intEnum_DeprecatedValues() {
        return (Vector<String>) this.extra3;
    }

    /**
     * the value returned in case there is no value in the database and no default value is indicated
     */
    public Object getEmptyValue() {
        switch (getIntegerType()) {
            case FieldDefinition._char:
            case FieldDefinition._charEnum:
            case FieldDefinition._text:
            case FieldDefinition._binary:
                return "";
            case FieldDefinition._boolean:
                return false; // FIXME check if this is true
            case FieldDefinition._date:
            case FieldDefinition._dateCreate:
            case FieldDefinition._dateModify:
                return emptyDate;
            case FieldDefinition._int:
            case FieldDefinition._intEnum:
                return emptyInt;
            case FieldDefinition._real:
                return emptyReal;
            default:
                return null;
        }
    }

    public Object getNull() {

        switch (getIntegerType()) {
            case FieldDefinition._char:
            case FieldDefinition._charEnum:
                return Pointer.NullString;
            case FieldDefinition._date:
            case FieldDefinition._dateCreate:
            case FieldDefinition._dateModify:
                return Pointer.NullDate;
            case FieldDefinition._int:
            case FieldDefinition._intEnum:
                return Pointer.NullInteger;
            case FieldDefinition._ptr:
            case FieldDefinition._ptrIndex:
            case FieldDefinition._ptrOne:
            case FieldDefinition._ptrRel:
            case FieldDefinition._setComplex:
                if (isFileType()) { // file is a transformed to a pointer type on MDD parsin
                    // but the binary input is on the name of the field, not field.content
                    return Pointer.NullText;
                } else {
                    return Pointer.Null;
                }
            case FieldDefinition._real:
                return Pointer.NullReal;
            case FieldDefinition._set:
            case FieldDefinition._setCharEnum:
            case FieldDefinition._setIntEnum:
                return Pointer.NullSet;
            case FieldDefinition._text:
            case FieldDefinition._binary:
                return Pointer.NullText;
            case FieldDefinition._boolean:
                return Pointer.NullBoolean;
            default:
                throw new RuntimeException("Unknown case handling for field type '" + this + "', integer type "
                        + getIntegerType());
        }
    }

    // moved from dateHandler
    public static final Date emptyDate;

    static {
        Calendar c = new GregorianCalendar(org.makumba.MakumbaSystem.getTimeZone());
        c.clear();
        c.set(1900, 0, 1);
        emptyDate = c.getTime();
    }

    // moved from intHandler
    static final Object emptyInt = new Integer(0);

    // moved from realHandler
    static final Object emptyReal = new Double(0d);

    /** the name of this handler, normally the same with the name of the field */
    public String getName() {
        return getDataName();
    }

    /** the data field this handler is associated to */
    public final String getDataName() {
        return name;
    }

    /** tells whether this field has a description originally */
    public boolean hasDescription() {
        return !description.equals(name);
    }

    /**
     * returns field's description, if present. If not present (null or "") it returns field name.
     */
    public String getDescription() {
        if (description == null) {
            return StringUtils.upperCaseBeginning(name);
        }
        if (description.trim().equals("") || description.trim().equals(name)) {
            return StringUtils.upperCaseBeginning(name);
        }
        return description.trim();
    }

    /** returns field's type */
    public String getType() {
        return type;
    }

    public boolean isIndexPointerField() {
        return getDataDefinition().getIndexPointerFieldName() == name;
    }

    /** returns field type's integer value */
    public int getIntegerType() {
        return (Integer) integerTypeMap.get(getType());
    }

    // should be set while parsing
    // intEnum has int, set has null, etc
    public String getDataType() {
        switch (getIntegerType()) {
            case FieldDefinition._char:
            case FieldDefinition._charEnum:
                return "char";
            case FieldDefinition._date:
                return "datetime";
            case FieldDefinition._dateCreate:
            case FieldDefinition._dateModify:
                return "timestamp";
            case FieldDefinition._int:
            case FieldDefinition._intEnum:
                return "int";
            case FieldDefinition._ptr:
            case FieldDefinition._ptrIndex:
            case FieldDefinition._ptrOne:
            case FieldDefinition._ptrRel:
                return "pointer";
            case FieldDefinition._real:
                return "real";
            case FieldDefinition._set:
                return "set";
            case FieldDefinition._setCharEnum:
                return "setchar";
            case FieldDefinition._setComplex:
                return "null";
            case FieldDefinition._setIntEnum:
                return "setint";
            case FieldDefinition._text:
                return "text";
            case FieldDefinition._binary:
                return "binary";
            case FieldDefinition._boolean:
                return "boolean";
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    // intEnum has int, set has null, etc
    public Class<?> getJavaType() {
        switch (getIntegerType()) {
            case FieldDefinition._char:
            case FieldDefinition._charEnum:
                return java.lang.String.class;
            case FieldDefinition._date:
            case FieldDefinition._dateCreate:
            case FieldDefinition._dateModify:
                return java.util.Date.class;
            case FieldDefinition._int:
            case FieldDefinition._intEnum:
                return java.lang.Integer.class;
            case FieldDefinition._ptr:
            case FieldDefinition._ptrIndex:
            case FieldDefinition._ptrOne:
            case FieldDefinition._ptrRel:
                return Pointer.class;
            case FieldDefinition._real:
                return java.lang.Double.class;
            case FieldDefinition._set:
            case FieldDefinition._setCharEnum:
            case FieldDefinition._setIntEnum:
                return java.util.Vector.class;
            case FieldDefinition._setComplex:
                return null;
            case FieldDefinition._text:
            case FieldDefinition._binary:
                return org.makumba.Text.class;
            case FieldDefinition._boolean:
                return java.lang.Boolean.class;
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    /** tells whether this field is fixed */
    public boolean isFixed() {
        return fixed;
    }

    /** tells whether this field is not null */
    public boolean isNotNull() {
        return notNull;
    }

    public boolean isNotEmpty() {
        return notEmpty;
    }

    /** tells whether this field is unique */
    public boolean isUnique() {
        return unique;
    }

    /** returns the default value of this field */
    public Object getDefaultValue() {
        if (defaultValue == null) {
            return getEmptyValue();
        }
        return defaultValue;
    }

    /**
     * works only for intEnum, charEnum, setintEnum, setcharEnum types
     */
    public Collection getValues() {
        switch (getIntegerType()) {
            case FieldDefinition._charEnum:
            case FieldDefinition._intEnum:
                return (Vector) this.extra1;
            case FieldDefinition._setCharEnum:
            case FieldDefinition._setIntEnum:
                return (Vector) getEnum().extra1;
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    /**
     * works only for intEnum, charEnum, setintEnum, setcharEnum types FIXME this does not work, because of a
     * {@link ClassCastException} when attempting to convert the extra2 informatino into a Vector of Strings
     */
    public Collection<String> getNames() {
        switch (getIntegerType()) {
            case FieldDefinition._charEnum:
            case FieldDefinition._intEnum:
                return (Vector) this.extra2;
            case FieldDefinition._setCharEnum:
            case FieldDefinition._setIntEnum:
                return (Vector) getEnum().extra2;
            default:
                throw new RuntimeException("getNames() only work for intEnum and charEnum");
        }
    }

    /**
     * works only for intEnum, charEnum, setintEnum, setcharEnum types
     */
    public int getEnumeratorSize() {
        switch (getIntegerType()) {
            case FieldDefinition._charEnum:
            case FieldDefinition._intEnum:
                return ((Vector) this.extra1).size();
            case FieldDefinition._setCharEnum:
            case FieldDefinition._setIntEnum:
                return ((Vector) getEnum().extra1).size();
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    /**
     * works only for intEnum type
     */
    public String getNameFor(int i) {
        switch (getIntegerType()) {
            case FieldDefinition._intEnum:
            case FieldDefinition._setIntEnum:
                return get_intEnum_NameFor(i);
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    // moved from intEnumHandler
    public String get_intEnum_NameFor(int n) {
        Vector<String> names = (Vector<String>) this.extra2;
        Vector<?> values = (Vector<?>) this.extra1;
        for (int i = 0; i < values.size(); i++) {
            if (values.elementAt(i).equals(n)) {
                return names.elementAt(i);
            }
        }
        throw new org.makumba.InvalidValueException(this, "Can't find a name for " + n + " in " + values
                + " with names " + names);
    }

    /**
     * works only for intEnum, charEnum, setintEnum, setcharEnum types
     */
    public String getNameAt(int i) {
        switch (getIntegerType()) {
            case FieldDefinition._charEnum:
                return ((Vector<String>) this.extra1).elementAt(i);
            case FieldDefinition._intEnum:
                return ((Vector<String>) this.extra2).elementAt(i);
            case FieldDefinition._setCharEnum:
                return ((Vector<String>) getEnum().extra1).elementAt(i);
            case FieldDefinition._setIntEnum:
                return ((Vector<String>) getEnum().extra2).elementAt(i);
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    /**
     * works only for int, intEnum, setintEnum types
     */
    public int getDefaultInt() {
        switch (getIntegerType()) {
            case FieldDefinition._int:
            case FieldDefinition._intEnum:
                return (Integer) getDefaultValue();
            case FieldDefinition._setIntEnum:
                return (Integer) getEnum().defaultValue;
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    /**
     * works only for intEnum, setintEnum types
     */
    public int getIntAt(int i) {
        switch (getIntegerType()) {
            case FieldDefinition._intEnum:
                return ((Integer) ((Vector) this.extra1).elementAt(i)).intValue();
            case FieldDefinition._setIntEnum:
                return ((Integer) ((Vector) getEnum().extra1).elementAt(i)).intValue();
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    /**
     * works only for char, text, binary, charEnum, setcharEnum types
     */
    public String getDefaultString() {
        switch (getIntegerType()) {
            case FieldDefinition._char:
            case FieldDefinition._charEnum:
            case FieldDefinition._text:
            case FieldDefinition._binary:
                return (String) getDefaultValue();
            case FieldDefinition._setCharEnum:
                return (String) getEnum().defaultValue;
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    /**
     * works only for char, charEnum, setcharEnum types
     */
    public int getWidth() {
        switch (getIntegerType()) {
            case FieldDefinition._char:
            case FieldDefinition._charEnum:
                return ((Integer) this.extra2).intValue();
            case FieldDefinition._setCharEnum:
                return ((Integer) getEnum().extra2).intValue();
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    // inserted 20050418
    public Object checkValueImpl(Object value) {
        switch (getIntegerType()) {
            case FieldDefinition._char:
                return check_char_ValueImpl(value);
            case FieldDefinition._charEnum:
                return check_charEnum_ValueImpl(value);
            case FieldDefinition._date:
            case FieldDefinition._dateCreate:
            case FieldDefinition._dateModify:
                return check_date_ValueImpl(value);
            case FieldDefinition._int:
                return check_int_ValueImpl(value);
            case FieldDefinition._intEnum:
                return check_intEnum_ValueImpl(value);
            case FieldDefinition._ptr:
            case FieldDefinition._ptrIndex:
            case FieldDefinition._ptrOne:
            case FieldDefinition._ptrRel:
            case FieldDefinition._setIntEnum:
                if (isFileType() && !(value instanceof Pointer)) {// file is a transformed to a pointer type on MDD
                    // parsing but the binary input is on the name of the field, not field.content
                    return check_binary_ValueImpl(value);
                } else {
                    return check_ptrIndex_ValueImpl(value);
                }
            case FieldDefinition._real:
                return check_real_ValueImpl(value);
            case FieldDefinition._set:
                return check_set_ValueImpl(value);
            case FieldDefinition._setCharEnum:
                return check_setcharEnum_ValueImpl(value);
            case FieldDefinition._setComplex:
                return check_setComplex_ValueImpl(value);
            case FieldDefinition._text:
                return check_text_ValueImpl(value);
            case FieldDefinition._binary:
            case FieldDefinition._file:
                return check_binary_ValueImpl(value);
            case FieldDefinition._boolean:
                return check_boolean_ValueImpl(value);
            default:
                throw new RuntimeException("Unknown case handling for field type '" + this + "', integer type "
                        + getIntegerType());
        }
    }

    // moved from charHandler
    public Object check_char_ValueImpl(Object value) {
        normalCheck(value);
        String s = (String) value;
        if (s.length() > getWidth()) {
            throw new InvalidValueException(this, "String too long for char[] field. Maximum width: " + getWidth()
                    + " given width " + s.length() + ".\n\tGiven value <" + s + ">");
        }
        return value;
    }

    // moved from charEnumHandler
    public Object check_charEnum_ValueImpl(Object value) {
        check_char_ValueImpl(value);

        Vector names = (Vector) this.extra1;

        for (int i = 0; i < names.size(); i++) {
            if (names.elementAt(i).equals(value)) {
                return value;
            }
        }
        throw new org.makumba.InvalidValueException(this, "value set to char enumerator (" + value
                + ") is not a member of " + names);
    }

    // moved from dateHandler
    public Object check_date_ValueImpl(Object value) {
        return normalCheck(value);
    }

    // moved from intHandler
    public Object check_int_ValueImpl(Object value) {
        // we allow Integer and Long types (Long might come e.g. from JSTL <ftm:parseNumber ...> which returns a Long
        if (!(value instanceof Integer || value instanceof Long)) {
            throw new org.makumba.InvalidValueException(this, getJavaType(), value);
        }
        if (value instanceof Integer) {
            return value;
        } else { // if it is a Long, we convert it to an Integer
            // FIXME: this might potentially mean losing some data, if the Long is too long for an Integer
            // Solution: Either makumba stores the date as long, or we throw an error if the value is too big?
            // See: http://bugs.best.eu.org/1071
            return ((Long) value).intValue();
        }
    }

    // moved from intEnumHandler
    public Object check_intEnum_ValueImpl(Object value) {
        Vector names = (Vector) this.extra2;
        Vector values = (Vector) this.extra1;
        if (value instanceof Integer) {
            for (int i = 0; i < values.size(); i++) {
                if (values.elementAt(i).equals(value)) {
                    return value;
                }
            }
            throw new org.makumba.InvalidValueException(this, "int value set to int enumerator (" + value
                    + ") is not a member of " + values);
        }
        if (!(value instanceof String)) {
            throw new org.makumba.InvalidValueException(this,
                    "int enumerators only accept values of type Integer or String. Value supplied (" + value
                            + ") is of type " + value.getClass().getName());
        }

        for (int i = 0; i < names.size(); i++) {
            if (names.elementAt(i).equals(value)) {
                return values.elementAt(i);
            }
        }

        throw new org.makumba.InvalidValueException(this, "string value set to int enumerator (" + value
                + ") is neither a member of " + names + " nor amember of " + values);
    }

    // moved from ptrIndexHandler
    public Object check_ptrIndex_ValueImpl(Object value) {
        if (value instanceof Pointer) {
            if (!((Pointer) value).getType().equals(getPointedType().getName())) {
                throw new InvalidValueException(this, getPointedType().getName(), (Pointer) value);
            }
            return value;
        }
        if (value instanceof String) {
            return new Pointer(getPointedType().getName(), (String) value);
        }
        throw new InvalidValueException(
                this,
                "Only java.lang.String and org.makumba.Pointer (or a java.util.Vector containing elements of those types) are assignable to makumba pointers, given value <"
                        + value + "> is of type " + value.getClass().getName());
    }

    // moved from realHandler
    public Object check_real_ValueImpl(Object value) {
        if (value instanceof Integer) {
            return value;
        }
        return normalCheck(value);
    }

    // moved from setHandler
    public Object check_set_ValueImpl(Object value) {
        try {
            // may be just a pointer
            Object o = check_ptrIndex_ValueImpl(value);
            Vector<Object> v = new Vector<Object>();
            if (o != null && o instanceof Pointer) {
                v.addElement(o);
            }
            return v;
        } catch (org.makumba.InvalidValueException ive) {
        }

        normalCheck(value);

        Vector<Object> v = (Vector) value;

        FieldDefinition ptr = getForeignTable().getFieldDefinition(getForeignTable().getIndexPointerFieldName());

        for (int i = 0; i < v.size(); i++) {
            if (v.elementAt(i) == null || v.elementAt(i).equals(org.makumba.Pointer.Null)) {
                throw new org.makumba.InvalidValueException(this, "set members cannot be null");
            }
            try {
                v.setElementAt(ptr.checkValue(v.elementAt(i)), i);
            } catch (org.makumba.InvalidValueException e) {
                throw new org.makumba.InvalidValueException(this, "the set member <" + v.elementAt(i)
                        + "> is not assignable to pointers of type " + getForeignTable().getName());
            }
        }
        return v;
    }

    // moved from setcharEnumHandler
    public Object check_setcharEnum_ValueImpl(Object value) {
        try {
            Object o = getEnum().checkValue(value);
            Vector<Object> v = new Vector<Object>();
            if (o != null && o instanceof String) {
                v.addElement(o);
            }
            return v;
        } catch (org.makumba.InvalidValueException ive) {
        }

        normalCheck(value);

        Vector<Object> v = (Vector) value;

        for (int i = 0; i < v.size(); i++) {
            if (v.elementAt(i) == null || v.elementAt(i).equals(org.makumba.Pointer.NullString)) {
                throw new org.makumba.InvalidValueException(this, "set members cannot be null");
            }
            v.setElementAt(getEnum().checkValue(v.elementAt(i)), i);
        }
        return v;
    }

    // moved from setComplexHandler
    public Object check_setComplex_ValueImpl(Object value) {
        throw new org.makumba.InvalidValueException(this, "subsets cannot be assigned directly");
    }

    // moved from textHandler
    public Object check_text_ValueImpl(Object value) {
        try {
            return Text.getText(value);
        } catch (InvalidValueException e) {
            throw new InvalidValueException(this, e.getMessage());
        }
    }

    public Object check_binary_ValueImpl(Object value) {
        try {
            return Text.getText(value);
        } catch (InvalidValueException e) {
            throw new InvalidValueException(this, e.getMessage());
        }
    }

    public Object check_boolean_ValueImpl(Object value) {
        if (value instanceof Boolean) {
            return value;
        }
        return normalCheck(value);

    }

    // moved from setcharEnumHandler and setintEnumHandler
    FieldInfo getEnum() {
        return (FieldInfo) ((DataDefinition) this.extra1).getFieldDefinition("enum");
    }

    // moved from ptrOneHandler
    public DataDefinition get_ptrOne_Subtable() {
        return (DataDefinition) this.extra1;
    }

    /**
     * works only for date type
     */
    public Date getDefaultDate() {
        switch (getIntegerType()) {
            case FieldDefinition._date:
            case FieldDefinition._dateCreate:
            case FieldDefinition._dateModify:
                return (Date) getDefaultValue();
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    /**
     * works only for ptr, ptrRel and set types
     */
    public DataDefinition getForeignTable() {
        switch (getIntegerType()) {
            case FieldDefinition._ptr:
            case FieldDefinition._ptrRel:
                return get_ptrRel_ForeignTable();
            case FieldDefinition._set:
                return get_set_ForeignTable();
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    // moved from prtRelHandler
    public DataDefinition get_ptrRel_ForeignTable() {
        return (DataDefinition) this.extra1;
    }

    // moved from setHandler
    public DataDefinition get_set_ForeignTable() {
        if (this.extra3 == null) {
            return pointerToForeign().getForeignTable();
        } else {
            return (DataDefinition) this.extra3; // manually made
        }
    }

    // moved from setHandler
    FieldDefinition pointerToForeign() {
        return getSubtable().getFieldDefinition(getSubtable().getFieldNames().elementAt(4));
    }

    /**
     * works only for ptrOne, set, setComplex, setcharEnum and setintEnum types
     * 
     * @return the subtable indicated in set or ptr definition
     */
    public DataDefinition getSubtable() {
        switch (getIntegerType()) {
            case FieldDefinition._ptrOne:
            case FieldDefinition._setCharEnum:
            case FieldDefinition._setComplex:
            case FieldDefinition._setIntEnum:
            case FieldDefinition._file:
                return get_ptrOne_Subtable();
            case FieldDefinition._set:
                return get_set_Subtable();
            default:
                throw new RuntimeException("Trying to get a sub-table for a '" + getType() + "' for field '" + name
                        + "'.");
        }
    }

    // moved from setHandler
    public DataDefinition get_set_Subtable() {
        return (DataDefinition) this.extra1;
    }

    /**
     * works only for all pointer and set types
     */
    public DataDefinition getPointedType() {
        switch (getIntegerType()) {
            case FieldDefinition._ptrIndex:
                return get_ptrIndex_PointedType();
            case FieldDefinition._ptrOne:
            case FieldDefinition._setCharEnum:
            case FieldDefinition._setComplex:
            case FieldDefinition._setIntEnum:
            case FieldDefinition._file:
                return get_ptrOne_PointedType();
            case FieldDefinition._ptrRel:
            case FieldDefinition._ptr:
            case FieldDefinition._set:
                return get_ptrRel_PointedType();
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    // moved from ptrIndexHandler
    public DataDefinition get_ptrIndex_PointedType() {
        return getDataDefinition();
    }

    // moved from ptrOneHandler
    public DataDefinition get_ptrOne_PointedType() {
        return getSubtable();
    }

    // moved from ptrRelHandler
    public DataDefinition get_ptrRel_PointedType() {
        return getForeignTable();
    }

    /**
     * works only for ptr and set types
     * 
     * @return title field of the record in the foreign table, as indicated in this field definition or in the
     *         respective foreign table record definition
     * @see #hasTitleFieldIndicated()
     */
    public String getTitleField() {
        switch (getIntegerType()) {
            case FieldDefinition._ptr:
            case FieldDefinition._set:
                return get_ptr_TitleField();
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    // moved from ptrHandler
    public String get_ptr_TitleField() {
        if (hasTitleFieldIndicated()) {
            return (String) this.extra2;
        }
        return getForeignTable().getTitleFieldName();
    }

    /**
     * works only for ptr and set types
     * 
     * @return wether the definition indicates a title field
     * @exception ClassCastException
     *                for other types
     */
    public boolean hasTitleFieldIndicated() {
        switch (getIntegerType()) {
            case FieldDefinition._ptr:
            case FieldDefinition._set:
                return has_ptr_TitleFieldIndicated();
            default:
                throw new RuntimeException("Shouldn't be here");
        }
    }

    // moved from ptrHandler
    public boolean has_ptr_TitleFieldIndicated() {
        return this.extra2 != null;
    }

    // moved from FieldHandler
    protected Object normalCheck(Object value) {
        if (!getJavaType().isInstance(value)) {
            throw new org.makumba.InvalidValueException(this, getJavaType(), value);
        }
        return value;
    }

    public boolean isDefaultField() {
        return getIntegerType() == _ptrIndex || getIntegerType() == _dateCreate || getIntegerType() == _dateModify;
    }

    public boolean shouldEditBySingleInput() {
        return !(getIntegerType() == _ptrOne || getIntegerType() == _setComplex);
    }

    public boolean isDateType() {
        return getIntegerType() == _date || getIntegerType() == _dateCreate || getIntegerType() == _dateModify;
    }

    public boolean isIntegerType() {
        return getIntegerType() == _int;
    }

    public boolean isRealType() {
        return getIntegerType() == _real;
    }

    public boolean isNumberType() {
        return isIntegerType() || isRealType();
    }

    public boolean isBinaryType() {
        return getIntegerType() == _binary;
    }

    public boolean isBooleanType() {
        return getIntegerType() == _boolean;
    }

    public boolean isFileType() {
        return extra1 != null && extra1 instanceof FileRecordInfo;
    }

    public boolean isSetType() {
        return getIntegerType() == _set || isInternalSet();
    }

    public boolean isEnumType() {
        return getIntegerType() == _intEnum || getIntegerType() == _charEnum;
    }

    public boolean isSetEnumType() {
        return getIntegerType() == _setIntEnum || getIntegerType() == _setCharEnum;
    }

    public boolean isStringType() {
        return getIntegerType() == _char || getIntegerType() == _text;
    }

    public boolean isInternalSet() {
        return getIntegerType() == _setComplex || getIntegerType() == _setIntEnum || getIntegerType() == _setCharEnum;
    }

    public boolean isPointer() {
        return getIntegerType() == _ptr;
    }

    public boolean isExternalSet() {
        return getIntegerType() == _set;
    }

    public boolean isComplexSet() {
        return getIntegerType() == _setComplex;
    }

    public void addValidationRule(Collection<ValidationRule> rules) {
        if (rules != null) {
            for (ValidationRule validationRule : rules) {
                addValidationRule(validationRule);
            }
        }
    }

    public void addValidationRule(ValidationRule rule) {
        validationRules.put(rule.getRuleName(), rule);
    }

    public Collection<ValidationRule> getValidationRules() {
        // we sort the rules, so that comparison rules come in the end
        ArrayList<ValidationRule> arrayList = new ArrayList<ValidationRule>(validationRules.values());
        Collections.sort(arrayList);
        return arrayList;
    }

    public String getStructure() {
        StringBuffer sb = new StringBuffer();
        sb.append("--- structure of " + getName() + "\n");

        sb.append("getName() " + getName() + "\n");
        sb.append("getDataDefinition() " + getDataDefinition().getName() + "\n");
        sb.append("isIndexPointerField() " + isIndexPointerField() + "\n");
        sb.append("getEmptyValue() " + getEmptyValue() + "\n");
        sb.append("getNull()" + getNull() + "\n");
        try {
            sb.append("hasDescription() " + hasDescription() + "\n");
        } catch (RuntimeException e) {
            sb.append("has invalid description");
        }
        sb.append("getDescription() " + getDescription() + "\n");
        sb.append("getType() " + getType() + "\n");
        sb.append("getIntegerType() " + getIntegerType() + "\n");
        sb.append("getDataType() " + getDataType() + "\n");
        sb.append("getJavaType() " + getJavaType() + "\n");
        sb.append("isFixed() " + isFixed() + "\n");
        sb.append("isNotNull() " + isNotNull() + "\n");
        sb.append("isNotEmpty() " + isNotEmpty() + "\n");
        sb.append("isUnique() " + isUnique() + "\n");
        sb.append("getDefaultValue() " + getDefaultValue() + "\n");
        sb.append("getDefaultString()\n");
        try {
            sb.append(getDefaultString() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not a string\n");
        }
        sb.append("getDefaultInt()\n");
        try {
            sb.append(getDefaultInt() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not an int: " + re.getMessage() + "\n");
        }
        sb.append("getDefaultDate()\n");
        try {
            sb.append(getDefaultDate() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not a date\n");
        }
        sb.append("getValues()\n");
        try {
            sb.append(getValues() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not an enum\n");
        }
        sb.append("getNames()\n");
        try {
            sb.append(getNames() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not an enum: " + re.getMessage() + "\n");
        }
        sb.append("getEnumeratorSize()\n");
        try {
            sb.append(getEnumeratorSize() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not an enum\n");
        }

        sb.append("getWidth()\n");
        try {
            sb.append(getWidth() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not a char\n");
        }

        sb.append("getForeignTable()\n");
        try {
            sb.append(((RecordInfo) getForeignTable()).getName() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not a ptr\n");
        }

        sb.append("getSubtable()\n");
        try {
            sb.append(((RecordInfo) getSubtable()).getStructure() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not a ptr: " + re.getMessage() + "\n");
        }

        sb.append("getPointedType()\n");
        try {
            sb.append(((RecordInfo) getPointedType()).getName() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not a ptr\n");
        }

        sb.append("getTitleField()\n");
        try {
            sb.append(getTitleField() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not a ptr\n");
        }

        sb.append("getDeprecatedValues()\n");
        try {
            sb.append(getDeprecatedValues() + "\n");
        } catch (RuntimeException re) {
            sb.append("was not an enum\n");
        }

        sb.append("isDefaultField()" + isDefaultField() + "\n");
        sb.append("shouldEditBySingleInput() " + shouldEditBySingleInput() + "\n");
        sb.append("isDateType() " + isDateType() + "\n");
        sb.append("isNumberType() " + isNumberType() + "\n");
        sb.append("isIntegerType() " + isIntegerType() + "\n");
        sb.append("isRealType() " + isRealType() + "\n");
        sb.append("isBinaryType() " + isBinaryType() + "\n");
        sb.append("isFileType() " + isFileType() + "\n");
        sb.append("isSetType() " + isSetType() + "\n");
        sb.append("isSetEnumType() " + isSetEnumType() + "\n");
        sb.append("isEnumType() " + isEnumType() + "\n");
        sb.append("isInternalSet() " + isInternalSet() + "\n");
        sb.append("isExternalSet() " + isExternalSet() + "\n");
        sb.append("isComplexSet() " + isComplexSet() + "\n");
        sb.append("isPointer() " + isPointer() + "\n");
        sb.append("isStringType() " + isStringType() + "\n");

        sb.append("---  end structure of " + getName());

        return sb.toString();
    }

    public String getNotANumberErrorMessage() {
        return null;
    }

    public String getNotNullErrorMessage() {
        return null;
    }

    public String getNotUniqueErrorMessage() {
        return null;
    }

    public String getNotEmptyErrorMessage() {
        return null;
    }

    public String getNotIntErrorMessage() {
        return null;
    }

    public String getNotRealErrorMessage() {
        return null;
    }

    public String getNotBooleanErrorMessage() {
        return null;
    }

}
