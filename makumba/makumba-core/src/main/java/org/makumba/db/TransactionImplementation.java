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
//  $Id: timestampFormatter.java 2568 2008-06-14 01:06:21Z rosso_nero $
//  $Name$
/////////////////////////////////////
package org.makumba.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.collections.CollectionUtils;
import org.makumba.Attributes;
import org.makumba.CompositeValidationException;
import org.makumba.DBError;
import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;
import org.makumba.FieldDefinition.FieldErrorMessageType;
import org.makumba.FieldValueDiff;
import org.makumba.InvalidFieldTypeException;
import org.makumba.LogicException;
import org.makumba.MakumbaError;
import org.makumba.NoSuchFieldException;
import org.makumba.NotUniqueException;
import org.makumba.Pointer;
import org.makumba.ProgrammerError;
import org.makumba.Transaction;
import org.makumba.UnauthenticatedException;
import org.makumba.commons.RuntimeWrappedException;
import org.makumba.providers.DataDefinitionProvider;
import org.makumba.providers.QueryProvider;
import org.makumba.providers.TransactionProvider;

/**
 * Abstract {@link Transaction}, with helper methods for both concrete implementations
 * 
 * @version $Id: TransactionImplementation.java,v 1.1 Jun 15, 2008 3:31:07 PM rudi Exp $
 */
public abstract class TransactionImplementation implements Transaction {

    protected DataDefinitionProvider ddp;

    protected QueryProvider qp;

    protected TransactionProvider tp;

    private Attributes contextAttributes;

    public TransactionImplementation(TransactionProvider tp) {
        this.tp = tp;
        this.ddp = DataDefinitionProvider.getInstance();
    }

    @Override
    public abstract void close();

    @Override
    public abstract void commit();

    @Override
    public abstract void rollback();

    @Override
    public abstract String getName();

    @Override
    public abstract void lock(String symbol);

    @Override
    public abstract void unlock(String symbol);

    /**
     * Executes an UPDATE statement or a DELETE FROM statement, depending on the value of set.
     * 
     * @param type
     *            the type on which to perform the operation
     * @param set
     *            the SET part of the query. if null, this performs a DELETE FROM statement
     * @param where
     *            the WHERE part of the query
     * @param args
     *            the query arguments
     * @return either (1) the row count for <code>INSERT</code>, <code>UPDATE</code>, or <code>DELETE</code> statements
     *         or (2) 0 for SQL statements that return nothing
     */
    protected abstract int executeUpdate(String type, String set, String where, Object args);

    /**
     * Delete the record pointed by the given pointer. If the pointer is a 1-1, the oringinal is set to null. All the
     * subrecords and subsets are automatically deleted.
     */
    @Override
    public void delete(Pointer ptr) {
        if (ptr == null) {
            throw new ProgrammerError("The pointer to be deleted should not be null");
        }
        DataDefinition ri = ddp.getDataDefinition(ptr.getType());
        FieldDefinition fi = ri.getParentField();

        // if this is a ptrOne, we nullify the pointer in the parent record
        if (fi != null && fi.getType().equals("ptrOne")) {
            executeUpdate(transformTypeName(fi.getDataDefinition().getName()) + " this", "this." + fi.getName() + "="
                    + getNullConstant(), "this." + fi.getName() + getPrimaryKeyName() + "=" + getParameterName(), ptr);
        }

        // then we do the rest of the delete job
        try {
            delete1(ptr);
        } catch (Throwable e) {
            if (e.getClass().getName().endsWith("ConstraintViolationException")) {
                throw new DBError(e);
            }
            if (e instanceof Error) {
                throw (Error) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeWrappedException(e);
        }

    }

    /**
     * Deletes in the form delete("general.Person p", "p=$1", params) NOTE that this method does not delete subsets and
     * subrecords
     * 
     * @return the number of records affected
     */
    @Override
    public int delete(String from, String where, Object parameters) {
        return executeUpdate(from, null, where, parameters);
    }

    @Override
    public abstract Vector<Dictionary<String, Object>> executeQuery(String OQL, Object parameterValues, int offset,
            int limit);

    @Override
    public abstract Vector<Dictionary<String, Object>> executeQuery(String OQL, Object parameterValues);

    @Override
    public TransactionProvider getTransactionProvider() {
        return this.tp;
    }

    @Override
    public Pointer insert(String type, Dictionary<String, Object> data) {

        // TODO: this does not support the DataTransformer possibility as for the Makumba DB.
        // Probably all those Makumba DB features should be placed in another place than the makumba DB.

        DataHolder dh = new DataHolder(this, data, type);
        dh.checkInsert();
        return dh.insert();

    }

    @Override
    public Vector<Pointer> insert(String type, Collection<Dictionary<String, Object>> data) {
        throw new MakumbaError("not implemented");
    }

    /**
     * Insert a record in a subset (1-N set) or subrecord (1-1 pointer) of the given record. For 1-1 pointers, if
     * another subrecord existed, it is deleted.
     * 
     * @return a Pointer to the inserted record
     */
    @Override
    public Pointer insert(Pointer base, String field, Dictionary<String, Object> data) {
        FieldDefinition fi = ddp.getDataDefinition(base.getType()).getFieldDefinition(field);
        if (fi == null) {
            throw new NoSuchFieldException(ddp.getDataDefinition(base.getType()), field);
        }
        if (fi.getType().equals("setComplex")) {
            data.put(fi.getSubtable().getSetOwnerFieldName(), base);
            return insert(fi.getSubtable().getName(), data);
        } else {
            throw new InvalidFieldTypeException(fi, "subset");
        }
    }

    @Override
    public int insertFromQuery(String type, String OQL, Object parameterValues) {
        int i = 0;
        try {
            i = insertFromQueryImpl(type, OQL, parameterValues);
        } catch (NotUniqueException nue) {
            treatNotUniqueException(type, nue);
        }
        return i;
    }

    protected abstract int insertFromQueryImpl(String type, String OQL, Object parameterValues);

    protected abstract StringBuffer writeReadQuery(Pointer p, Enumeration<String> e);

    /** change the record pointed by the given pointer. Only fields indicated are changed to the respective values */
    @Override
    public int update(Pointer ptr, java.util.Dictionary<String, Object> fieldsToChange) {
        DataHolder dh = new DataHolder(this, fieldsToChange, ptr.getType());
        dh.checkUpdate(ptr);
        int updated = 0;

        try {
            updated = dh.update(ptr);
        } catch (NotUniqueException nue) {
            treatNotUniqueException(ptr.getType(), nue);
        }

        return updated;
    }

    @Override
    public ArrayList<FieldValueDiff> updateWithValueDiff(Pointer ptr, Dictionary<String, Object> fieldsToChange) {
        DataDefinition dd = DataDefinitionProvider.getInstance().getDataDefinition(ptr.getType());

        ArrayList<String> fields = new ArrayList<String>();
        ArrayList<String> sets = new ArrayList<String>();

        // read the current values for the non-set fields of the MDD from the DB, by composing a query
        StringBuilder proj = new StringBuilder();
        for (Enumeration<String> e = fieldsToChange.keys(); e.hasMoreElements();) {
            String fldName = e.nextElement();
            FieldDefinition fieldDefinition = dd.getFieldOrPointedFieldDefinition(fldName);

            if (fieldDefinition.isSetType()) {
                // set types will be treated later
                sets.add(fldName);
            } else {
                if (proj.length() > 0) {
                    proj.append(", ");
                }
                proj.append("o.").append(fldName);
                fields.add(fldName);
            }
        }

        // execute the query
        String query = "SELECT " + proj.toString() + " FROM " + ptr.getType() + " o WHERE o=$1";
        Vector<Dictionary<String, Object>> oldValuesVec = executeQuery(query.toString(), ptr);
        Dictionary<String, Object> oldValues = oldValuesVec.firstElement();

        ArrayList<FieldValueDiff> res = new ArrayList<FieldValueDiff>();

        // process the normal changes
        for (int col = 0; col < fields.size(); col++) {
            String fldName = fields.get(col);
            FieldDefinition fieldDefinition = dd.getFieldOrPointedFieldDefinition(fldName);
            Object newValue = fieldsToChange.get(fldName);
            Object oldValue = oldValues.get("col" + (col + 1));
            if (!newValue.equals(oldValue)) {
                res.add(new FieldValueDiff(fldName, fieldDefinition, oldValue, newValue));
            }
        }

        // process the changes on sets
        for (String setFieldName : sets) {
            FieldDefinition fieldDefinition = dd.getFieldOrPointedFieldDefinition(setFieldName);
            Vector<?> oldValue = readSetValues(ptr, setFieldName);
            Object newValue = fieldsToChange.get(setFieldName);
            if (!(newValue.equals(oldValue) || newValue == Pointer.NullSet && oldValue.size() == 0)) {
                res.add(new FieldValueDiff(setFieldName, fieldDefinition, oldValue, newValue));
            }
        }

        update(ptr, fieldsToChange);
        return res;
    }

    @Override
    public int updateSet(Pointer basePointer, String setName, Collection<?> addElements, Collection<?> removeElements) {
        // read the set's current values from db
        final Vector<Pointer> setElements = readExternalSetValues(basePointer, setName);

        // get the set type
        final FieldDefinition fdSet = DataDefinitionProvider.getInstance().getDataDefinition(basePointer.getType()).getFieldDefinition(
            setName);
        final DataDefinition setDD = fdSet.getPointedType();
        final FieldDefinition setDDPointer = setDD.getFieldDefinition(setDD.getIndexPointerFieldName());

        // remove from the set the elements that should be deleted
        if (removeElements != null) {
            for (Object element : removeElements) {
                Pointer ptr = (Pointer) setDDPointer.checkValue(element); // check type & convert to pointer
                setElements.remove(ptr);
            }
        }

        // add the new elements to the set
        if (addElements != null) {
            for (Object element : addElements) {
                Pointer ptr = (Pointer) setDDPointer.checkValue(element); // check type & convert to pointer
                setElements.add(ptr);
            }
        }

        Hashtable<String, Object> d = new Hashtable<String, Object>(1);
        d.put(setName, setElements);
        return update(basePointer, d);
    }

    @Override
    public Vector<Pointer> readExternalSetValues(Pointer basePointer, String setName) {
        return readSetValues(basePointer, setName);
    }

    @Override
    public Vector<Integer> readIntEnumValues(Pointer basePointer, String setName) {
        return readSetValues(basePointer, setName);
    }

    @Override
    public Vector<String> readCharEnumValues(Pointer basePointer, String setName) {
        return readSetValues(basePointer, setName);
    }

    protected <T> Vector<T> readSetValues(Pointer basePointer, String setName) {
        String label = "setElement";
        Vector<Dictionary<String, Object>> v = executeQuery(
            "SELECT " + label + " as " + label + " FROM " + basePointer.getType() + " o, o." + setName + " " + label
                    + " WHERE o=$1", basePointer);

        Vector<T> vec = new Vector<T>();
        for (Dictionary<String, Object> dictionary : v) {
            @SuppressWarnings("unchecked")
            T e = (T) dictionary.get(label);
            vec.add(e);
        }
        return vec;
    }

    private void treatNotUniqueException(String type, NotUniqueException nue) {
        DataDefinition dd = ddp.getDataDefinition(type);
        CompositeValidationException cve = new CompositeValidationException();

        if (nue.getFields().size() == 1) {
            // see if we have a custom message for this field
            FieldDefinition fd = dd.getFieldDefinition(nue.getFields().keySet().iterator().next());
            if (fd.getErrorMessage(FieldErrorMessageType.NOT_UNIQUE) == null) {
                cve.addException(new NotUniqueException(fd, nue.getFields().get(fd.getName())));
            } else {
                cve.addException(new NotUniqueException(fd.getErrorMessage(FieldErrorMessageType.NOT_UNIQUE)));
            }
        } else {
            // multi-field uniqueness exception
            for (DataDefinition.MultipleUniqueKeyDefinition m : dd.getMultiFieldUniqueKeys()) {
                if (CollectionUtils.isEqualCollection(Arrays.asList(m.getFields()), nue.getFields().keySet())) {
                    cve.addException(new NotUniqueException(m.getErrorMessage()));
                }
            }
        }
        throw cve;
    }

    /**
     * updates in the form update("general.Person p", "p.birthdate=$1", "p=$2", params) NOTE that this method does not
     * delete subrecords if their pointers are nullified
     * 
     * @return the number of records affected
     */
    @Override
    public int update(String from, String set, String where, Object parameters) {
        return executeUpdate(from, set, where, parameters);
    }

    @Override
    public Dictionary<String, Object> read(Pointer p, Object flds) {

        Enumeration<String> e = extractReadFields(p, flds);

        StringBuffer sb = writeReadQuery(p, e);

        Vector<Dictionary<String, Object>> v = executeReadQuery(p, sb);

        if (v.size() == 0) {
            return null;
        }
        if (v.size() > 1) {
            throw new org.makumba.MakumbaError("MAKUMBA DATABASE INCOSISTENT: Pointer not unique: " + p);
        }
        Dictionary<String, Object> d = v.elementAt(0);
        Hashtable<String, Object> h = new Hashtable<String, Object>(13);
        for (Enumeration<String> en = d.keys(); en.hasMoreElements();) {
            Object o = en.nextElement();
            h.put((String) o, d.get(o));
        }
        return h;
    }

    protected Enumeration<String> extractReadFields(Pointer p, Object flds) throws ProgrammerError {
        Enumeration<String> e = null;
        if (flds == null) {
            DataDefinition ri = ddp.getDataDefinition(p.getType());
            Vector<String> v = new Vector<String>();
            for (FieldDefinition fd : ri.getFieldDefinitions()) {
                if (!fd.getType().startsWith("set")) {
                    v.addElement(fd.getName());
                }
            }
            e = v.elements();
        } else if (flds instanceof Vector) {
            @SuppressWarnings("unchecked")
            Vector<String> vector = (Vector<String>) flds;
            e = vector.elements();
        } else if (flds instanceof Enumeration) {
            @SuppressWarnings("unchecked")
            Enumeration<String> flds2 = (Enumeration<String>) flds;
            e = flds2;
        } else if (flds instanceof String[]) {
            Vector<String> v = new Vector<String>();
            String[] fl = (String[]) flds;
            for (String element : fl) {
                v.addElement(element);
            }
            e = v.elements();
        } else if (flds instanceof String) {
            Vector<String> v = new Vector<String>();
            v.add((String) flds);
            e = v.elements();
        } else {
            throw new ProgrammerError("read() argument must be Enumeration, Vector, String[], String or null");
        }
        return e;
    }

    protected abstract Vector<Dictionary<String, Object>> executeReadQuery(Pointer p, StringBuffer sb);

    public void delete1(Pointer ptr) {
        String ptrDD = ptr.getType();
        DataDefinition ri = ddp.getDataDefinition(ptrDD);
        Object param[] = { ptr };

        // FIXME: deleting the ptrOnes and set entries could potentially be skipped, by automatically creating

        // delete the ptrOnes
        Vector<String> ptrOnes = new Vector<String>();

        for (FieldDefinition fd : ri.getFieldDefinitions()) {
            if (fd.getType().equals("ptrOne")) {
                ptrOnes.addElement(fd.getName());
            }
        }

        if (ptrOnes.size() > 0) {
            Dictionary<String, Object> d = read(ptr, ptrOnes);
            for (Enumeration<Object> e = d.elements(); e.hasMoreElements();) {
                delete((Pointer) e.nextElement());
            }
        }
        // delete all the subfields
        for (FieldDefinition fi : ri.getFieldDefinitions()) {
            if (fi.getType().startsWith("set")) {
                if (fi.getType().equals("setComplex")) {
                    // recursively process all set entries, to delete their subSets and ptrOnes
                    Vector<Dictionary<String, Object>> v = executeQuery(
                        "SELECT pointedType" + getPrimaryKeyName() + " as pointedType FROM " + ptr.getType() + " ptr "
                                + getSetJoinSyntax() + " ptr." + fi.getName() + " pointedType WHERE ptr"
                                + getPrimaryKeyName() + "=" + getParameterName(), ptr);
                    for (Dictionary<String, Object> dictionary : v) {
                        Pointer p = (Pointer) dictionary.get("pointedType");
                        delete1(p);
                    }
                    executeUpdate(
                        transformTypeName(fi.getSubtable().getName()) + " this",
                        null,
                        "this."
                                + transformTypeName(fi.getSubtable().getFieldDefinition(
                                    fi.getSubtable().getSetOwnerFieldName()).getName()) + getPrimaryKeyName() + "= "
                                + getParameterName(), param);
                } else {
                    tp.getCRUD().deleteSet(this, ptr, fi);
                }
            }
        }
        // delete the record
        executeUpdate(transformTypeName(ptrDD) + " this", null, "this." + getPrimaryKeyName(ptrDD) + "="
                + getParameterName(), ptr);
    }

    public String getSetJoinSyntax() {
        return ",";
    }

    /**
     * Takes a heterogeneous argument object (coming from the BL API or the view layer) and transforms it into a
     * Map<String, Object>, taking into account possible context attributes (session, request, and other bundled makumba
     * {@link Attributes})
     */
    protected Map<String, Object> paramsToMap(Object args) {
        final Map<String, Object> m = paramsToMap1(args);
        if (contextAttributes == null) {
            return m;
        }
        return new HashMap<String, Object>() {

            private static final long serialVersionUID = 1L;

            private Attributes contextAttributesCopy;

            @Override
            public Object get(Object key) {
                Object o = m.get(key);
                if (o != null) {
                    return o;
                }
                try {

                    if (contextAttributesCopy == null) {
                        contextAttributesCopy = contextAttributes;
                    }

                    o = contextAttributesCopy.getAttribute((String) key);
                    if (o == null && contextAttributesCopy.hasAttribute("" + key + "_null")) {
                        o = Pointer.Null;
                    }
                    return o;
                } catch (UnauthenticatedException e) {
                    // we need to pass on a potential UnauthenticatedException that might stem from actor lookup
                    throw new RuntimeWrappedException(e);
                } catch (LogicException e) {
                    return null;
                }
            }

        };
    }

    protected Map<String, Object> paramsToMap1(Object args) {
        if (args instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> args2 = (Map<String, Object>) args;
            return args2;
        }
        Map<String, Object> ret = new HashMap<String, Object>();
        if (args == null) {
            return ret;
        }
        if (args instanceof Collection) {
            args = ((Collection<?>) args).toArray();
        }
        if (args instanceof Object[]) {
            for (int j = 0; j < ((Object[]) args).length; j++) {
                ret.put("" + (j + 1), ((Object[]) args)[j]);
            }
            return ret;
        }
        ret.put("1", args);
        return ret;
    }

    public String transformTypeName(String name) {
        return name;
    }

    public String getParameterName() {
        return "$1";
    }

    public String getPrimaryKeyName() {
        return "";
    }

    public String getPrimaryKeyName(String ptrDD) {
        return ddp.getDataDefinition(ptrDD).getIndexPointerFieldName();
    }

    public abstract String getNullConstant();

    @Override
    public abstract String getDataSource();

    public void setContext(Attributes a) {
        contextAttributes = a;
    }
}
