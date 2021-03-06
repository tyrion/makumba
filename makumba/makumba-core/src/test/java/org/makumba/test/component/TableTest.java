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

package org.makumba.test.component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;
import org.makumba.FieldValueDiff;
import org.makumba.MakumbaError;
import org.makumba.Pointer;
import org.makumba.Text;
import org.makumba.Transaction;
import org.makumba.commons.ClassResource;
import org.makumba.commons.CollectionUtils;
import org.makumba.providers.DataDefinitionProvider;
import org.makumba.providers.TransactionProvider;
import org.makumba.test.util.MakumbaTestData;

/**
 * Testing table operations, using new MDD parser
 * 
 * @author Cristian Bogdan
 * @author Manuel Bernhardt <manuel@makumba.org>
 */
public class TableTest extends TestCase {

    static Transaction db;

    public TableTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TableTest.class);
    }

    @Override
    public void setUp() {
        TransactionProvider tp = TransactionProvider.getInstance();
        db = tp.getConnectionTo("testDatabase");
    }

    @Override
    public void tearDown() {
        db.close();
    }

    static Pointer ptr, ptr1;

    static Pointer fptr1, fptr2, fptr;

    static String[] personFields = { "TS_modify", "TS_create", "extraData", "birthdate" };

    static String[] ptrOneFields = { "something" };

    static String[] subsetFields = { "description" };

    static Dictionary<String, Object> pc, pc1;

    static Pointer ptrOne;

    static Pointer set1, set2;

    String readPerson = "SELECT p.indiv.name AS name, p.indiv.surname AS surname, p.birthdate AS birthdate, p.TS_modify as TS_modify, p.TS_create as TS_create, p.extraData.something as something, p.extraData as extraData FROM test.Person p WHERE p= $1";

    String readPersonPtr = "SELECT p.indiv.name AS name, p.indiv.surname AS surname, p.birthdate AS birthdate, p.TS_modify as TS_modify, p.TS_create as TS_create, p.extraData.something as something, p.extraData as extraData FROM test.Person p WHERE p IN ($1)";

    String readPerson1 = "SELECT p.indiv.name AS name, p.indiv.surname AS surname, p.birthdate AS birthdate, p.weight as weight, p.TS_modify as TS_modify, p.TS_create as TS_create, p.extraData.something as something, p.extraData as extraData, p.comment as comment, p.picture AS picture FROM test.Person p WHERE p= $1";

    String readPerson2 = "SELECT p.indiv.name AS name, p.indiv.surname AS surname, p.birthdate AS birthdate, p.weight as weight, p.brother as brother, p.TS_modify as TS_modify, p.TS_create as TS_create, p.extraData.something as something, p.extraData as extraData, p.comment as comment, p.picture AS picture FROM test.Person p WHERE p= $1";

    String readToy1 = "SELECT t.name AS name, t.color AS color, t.serial AS serial FROM test.validMdds.AllTheToysThatThisOrganisationPossiblyHasAtTheirDisposalForTheirMembers t WHERE t = $1";

    String readToy2 = "SELECT t.name AS name, t.color AS color, t.serial AS serial, t.relatedToy AS relatedToy FROM test.validMdds.AllTheToysThatThisOrganisationPossiblyHasAtTheirDisposalForTheirMembers t WHERE t = $1";

    String readIntSet = "SELECT i as member FROM test.Person p join p.intSet i WHERE p=$1 ORDER BY i";

    String readCharSet = "SELECT c as member FROM test.Person p, p.charSet c WHERE p=$1 ORDER BY c";

    static InputStream getExampleData() {
        try {
            return new BufferedInputStream(ClassResource.get("mdd-corpus.zip").openStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void testQueryValidMdds() {
        Vector<String> v = org.makumba.MakumbaSystem.mddsInDirectory("test/validMdds");
        DataDefinitionProvider ddp = DataDefinitionProvider.getInstance();

        Vector<String> errors = new Vector<String>();
        for (int i = 0; i < v.size(); i++) {
            try {
                db.executeQuery("SELECT t FROM test.validMdds." + v.elementAt(i) + " t", null);

                List<FieldDefinition> fields = ddp.getDataDefinition("test.validMdds." + v.elementAt(i)).getFieldDefinitions();
                String what = "";
                for (FieldDefinition fd : fields) {
                    String ftype = ddp.getDataDefinition("test.validMdds." + v.elementAt(i)).getFieldDefinition(
                        fd.getName()).getDataType();
                    // System.out.println(fname+": "+ftype);
                    if (ftype != null && !ftype.equals("null") && !ftype.startsWith("set")) {
                        // fields
                        what = what + (what.length() > 0 ? ", " : "") + "t." + fd.getName();
                    }

                }
                // System.out.println(what);
                if (what.length() > 0) {
                    db.executeQuery("SELECT " + what + " FROM test.validMdds." + v.elementAt(i) + " t", null);
                }
            } catch (Exception e) {
                errors.add("\n ." + (errors.size() + 1) + ") Error querying valid MDD <" + v.elementAt(i) + ">:\n\t "
                        + e);
            }
        }
        if (errors.size() > 0) {
            fail("\n  Tested " + v.size() + " valid MDDs, of which " + errors.size() + " cant be used for DB queries:"
                    + errors.toString());
        }

    }

    public void testInsert() {
        Hashtable<String, Object> p = new Hashtable<String, Object>();
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(1977, 2, 5);
        Date birth = c.getTime();

        Text comment = new Text("Itrntinliztin");

        p.put("birthdate", birth);
        p.put("comment", comment);
        p.put("picture", new Text(getExampleData()));

        p.put("weight", new Double(85.7d));

        p.put("indiv.name", "john");
        p.put("indiv.surname", "doe");
        p.put("extraData.something", "else");

        Vector<Integer> setintElem = CollectionUtils.toVector(1, 0);
        p.put("intSet", setintElem);

        Vector<String> setcharElem = CollectionUtils.toVector("f", "e");
        p.put("charSet", setcharElem);

        long before = System.currentTimeMillis();
        ptr = db.insert("test.Person", p);
        long after = System.currentTimeMillis();

        assertNotNull(ptr);
        assertEquals(ptr.getType(), "test.Person");

        Vector<Dictionary<String, Object>> v = db.executeQuery(readPerson1, ptr);

        assertEquals(1, v.size());

        Vector<Pointer> listArg = new Vector<Pointer>();
        // sending four params ensures that offset+limit+param is surpassed so we can test if a multiple param is set
        // for $1
        listArg.addAll(Arrays.asList(new Pointer[] { ptr, ptr, ptr, ptr }));
        Vector<Dictionary<String, Object>> v1 = db.executeQuery(readPersonPtr, new Object[] { listArg });
        assertEquals(1, v1.size());

        pc = v.elementAt(0);

        ptrOne = (Pointer) pc.get("extraData");
        assertEquals("Name", "john", pc.get("name"));
        assertEquals("Surname", "doe", pc.get("surname"));
        assertEquals("Weight(real)", new Double(85.7d), pc.get("weight"));
        assertEquals("Birthdate", birth, pc.get("birthdate"));
        assertEquals("Something else", "else", pc.get("something"));
        assertEquals("Picture", pc.get("picture"), new Text(getExampleData()));
        assertNotNull(ptrOne);

        v = db.executeQuery(readIntSet, ptr);
        assertEquals(2, v.size());
        assertEquals(new Integer(0), v.elementAt(0).get("member"));
        assertEquals(new Integer(1), v.elementAt(1).get("member"));

        v = db.executeQuery(readCharSet, ptr);
        assertEquals(v.size(), 2);
        assertEquals("e", v.elementAt(0).get("member"));
        assertEquals("f", v.elementAt(1).get("member"));

        Date create = (Date) pc.get("TS_create");
        assertEquals(create, pc.get("TS_modify"));
        // should be < and > but ....
        // http://bugs.mysql.com/bug.php?id=8523
        assertTrue(before <= create.getTime() + 1000);
        assertTrue(after >= create.getTime() - 1000);

        checkInternationalizedField(comment);
    }

    protected void checkInternationalizedField(Text comment) {
        assertEquals("Comment text", pc.get("comment").toString(), comment.getString());
    }

    public void testForeignKeys() {

        // try to delete brother = that ID
        // try to delete the other brother

        // insert the first person
        Hashtable<String, Object> p = new Hashtable<String, Object>();

        // insert two languages so the unique constraint on language will not be broken on databases that don't ignore
        // nulls in unique
        p.put("name", "vlach");
        Pointer lang = db.insert("test.Language", p);
        p.put("name", "urdu");
        Pointer lang1 = db.insert("test.Language", p);
        p.clear();

        Text comment = new Text("Hello world!!!!");

        p.put("comment", comment);

        p.put("indiv.name", "john");
        p.put("indiv.surname", "doe_1");
        p.put("extraData.something", "else");

        // avoid unique issues for databases that don't ignore null in unique
        p.put("uniqPtr", lang);
        p.put("age", 21);
        p.put("email", "dummy@xxx");
        p.put("uniqDate", new Date());
        p.put("uniqInt", 13);
        p.put("uniqChar", "unique I mean");

        fptr = db.insert("test.Person", p);

        // check if he got inserted
        assertNotNull(fptr);
        assertEquals(fptr.getType(), "test.Person");

        Vector<Dictionary<String, Object>> v = db.executeQuery(readPerson2, fptr);
        // System.out.println(v.size());
        assertEquals(1, v.size());

        // insert the second person (brother)
        p = new Hashtable<String, Object>();

        comment = new Text("Itrntinliztin");

        p.put("comment", comment);
        p.put("brother", fptr);
        p.put("indiv.name", "john");
        p.put("indiv.surname", "doe_2");
        p.put("extraData.something", "else");

        // avoid unique issues for databases that don't ignore null in unique
        p.put("uniqPtr", lang1);
        p.put("age", 22);
        p.put("email", "dummy@yyy");
        p.put("uniqDate", new Date(new Date().getTime() + 2000));
        p.put("uniqInt", 14);
        p.put("uniqChar", "unique I mean really");

        fptr1 = db.insert("test.Person", p);
        assertNotNull(fptr1);
        assertEquals(fptr.getType(), "test.Person");

        // check if it links to the first one correctly
        v = db.executeQuery(readPerson2, fptr1);

        assertEquals(1, v.size());

        pc = v.elementAt(0);

        fptr2 = (Pointer) pc.get("brother");
        assertNotNull(fptr2);
        assertEquals("Brother", fptr2, fptr);

        // try to delete the first guy (who was set as a brother. should fail)
        try {
            db.delete(fptr);
            // we could delete him... the foreign keys don't work
            assertTrue(false);
        } catch (org.makumba.DBError e) {
        }

        // try to delete the second guy
        db.delete(fptr1);

        // delete the first guy again, this time he shouldn't be linked to from anywhere
        db.delete(fptr);

        // cleanup the inserted languages
        db.delete(lang);
        db.delete(lang1);
    }

    public void testForeignKeysWithLongMDDName() {
        // try to delete toy = that ID
        // try to delete the other toy

        // insert the first toy
        Hashtable<String, Object> r = new Hashtable<String, Object>();

        r.put("name", "car");
        r.put("color", "red");
        r.put("serial", "mak111");

        fptr = db.insert("test.validMdds.AllTheToysThatThisOrganisationPossiblyHasAtTheirDisposalForTheirMembers", r);

        // check if he got inserted
        assertNotNull(fptr);
        assertEquals(fptr.getType(),
            "test.validMdds.AllTheToysThatThisOrganisationPossiblyHasAtTheirDisposalForTheirMembers");

        Vector<Dictionary<String, Object>> v = db.executeQuery(readToy1, fptr);
        // System.out.println(v.size());
        assertEquals(1, v.size());

        // insert the second toy
        r = new Hashtable<String, Object>();

        r.put("relatedToy", fptr);
        r.put("name", "helicopter");
        r.put("color", "rosa");
        r.put("serial", "mak121");

        fptr1 = db.insert("test.validMdds.AllTheToysThatThisOrganisationPossiblyHasAtTheirDisposalForTheirMembers", r);
        assertNotNull(fptr1);
        assertEquals(fptr.getType(),
            "test.validMdds.AllTheToysThatThisOrganisationPossiblyHasAtTheirDisposalForTheirMembers");

        // check if it links to the first one correctly
        v = db.executeQuery(readToy2, fptr1);

        assertEquals(1, v.size());

        pc = v.elementAt(0);

        fptr2 = (Pointer) pc.get("relatedToy");
        assertNotNull(fptr2);
        assertEquals("relatedToy", fptr2, fptr);

        // try to delete the first guy (who was set as a related toy. should fail)
        try {
            db.delete(fptr);
            // we could delete him... the foreign keys don't work
            assertTrue(false);
        } catch (org.makumba.DBError e) {
        }

        // try to delete the second guy
        db.delete(fptr1);

        // delete the first guy again, this time he shouldn't be linked to from anywhere
        db.delete(fptr);

    }

    static String subsetQuery = "SELECT a.description, a, a.description, a.sth.aaa FROM test.Person p, p.address a WHERE p=$1 ORDER BY a.description";

    public void testSetInsert() {
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("description", "home");
        p.put("sth.aaa", "bbb");

        set1 = db.insert(ptr, "address", p);

        assertNotNull(set1);
        Vector<Dictionary<String, Object>> v = db.executeQuery(subsetQuery, ptr);
        assertEquals(1, v.size());
        assertEquals("home", v.elementAt(0).get("col1"));
        assertEquals(set1, v.elementAt(0).get("col2"));
        assertEquals("home", v.elementAt(0).get("col3"));
        assertEquals("bbb", v.elementAt(0).get("col4"));

        p.put("description", "away");

        // avoid unique issues for databases that don't ignore null in unique
        p.put("sth.bbb", "xxx");
        p.put("streetno", "10");

        // now the test...
        set2 = db.insert(ptr, "address", p);
        assertNotNull(set2);
        assertEquals("away", db.read(set2, subsetFields).get("description"));
        v = db.executeQuery(subsetQuery, ptr);
        assertEquals(2, v.size());
        assertEquals("away", v.elementAt(0).get("col1"));
        assertEquals(set2, v.elementAt(0).get("col2"));
        assertEquals("home", v.elementAt(1).get("col1"));
        assertEquals(set1, v.elementAt(1).get("col2"));
    }

    public void testSetMemberUpdate() {
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("description", "somewhere");

        db.update(set2, p);

        Vector<Dictionary<String, Object>> v = db.executeQuery(subsetQuery, ptr);

        assertEquals("somewhere", db.read(set2, subsetFields).get("description"));
        v = db.executeQuery(subsetQuery, ptr);
        assertEquals(v.size(), 2);
        assertEquals("home", v.elementAt(0).get("col1"));
        assertEquals(set1, v.elementAt(0).get("col2"));
        assertEquals("somewhere", v.elementAt(1).get("col1"));
        assertEquals(set2, v.elementAt(1).get("col2"));
    }

    public void testSetMemberDelete() {
        db.delete(set1);
        assertNull(db.read(set1, subsetFields));
        Vector<Dictionary<String, Object>> v = db.executeQuery(subsetQuery, ptr);
        assertEquals(1, v.size());
        assertEquals("somewhere", v.elementAt(0).get("col1"));
        assertEquals(set2, v.elementAt(0).get("col2"));

        // we put it back
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("description", "home");

        set1 = db.insert(ptr, "address", p);
    }

    public void testSubrecordUpdate() {
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("something", "else2");

        db.update(ptrOne, p);

        Dictionary<String, Object> d = db.read(ptr, personFields);
        assertNotNull(d);
        assertEquals(ptrOne, d.get("extraData"));

        d = db.read(ptrOne, ptrOneFields);
        assertNotNull(d);
        assertEquals("else2", d.get("something"));
    }

    static String[][] languageData = { { "English", "en" }, { "French", "fr" }, { "German", "de" },
            { "Italian", "it" }, { "Spanish", "sp" } };

    static String[] toInsert = { "German", "Italian" };

    static String langQuery = "SELECT l FROM test.Language l WHERE l.name=$1";

    // using join to avoid the HQL default left join
    // TODO: check why doesn't the default MQL left join blow here
    static String speaksQuery = "SELECT l as k, l.name as name FROM test.Person p join p.speaks l WHERE p=$1";

    static String checkSpeaksQuery = "SELECT l, l.Language FROM test.Person__speaks l WHERE l.Person=$1";

    void workWithSet(String[] t) {
        Vector<Object> v = new Vector<Object>();
        for (String element : t) {
            v.addElement(db.executeQuery(langQuery, element).elementAt(0).get("col1"));
        }

        Hashtable<String, Object> dt = new Hashtable<String, Object>();
        dt.put("speaks", v);
        db.update(ptr, dt);

        Vector<Dictionary<String, Object>> result = db.executeQuery(speaksQuery, ptr);
        Vector<Dictionary<String, Object>> result1 = db.executeQuery(checkSpeaksQuery, ptr);

        assertEquals(t.length, result.size());
        assertEquals(t.length, result1.size());

        for (String element : t) {
            for (int j = 0; j < result.size(); j++) {
                Dictionary<String, Object> d = result.elementAt(j);
                if (d.get("name").equals(element)) {
                    for (int k = 0; j < result1.size(); k++) {
                        if (result1.elementAt(k).get("col2").equals(d.get("k"))) {
                            result1.removeElementAt(k);
                            break;
                        }
                    }
                    result.removeElementAt(j);
                    break;
                }
            }
        }
        assertEquals(0, result.size());
        assertEquals(0, result1.size());
    }

    public void testSetUpdate() {
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        if (db.executeQuery("SELECT l FROM test.Language l", null).size() == 0) {
            for (String[] element : languageData) {
                p.put("name", element[0]);
                p.put("isoCode", element[1]);
                db.insert("test.Language", p);
            }
        }
        p = new Hashtable<String, Object>();

        workWithSet(toInsert);
    }

    static String[] toInsert2 = { "English", "Italian", "French" };

    public void testSetUpdate2() {
        workWithSet(toInsert2);
    }

    static String[] toInsert3 = { "English", "German", "French" };

    public void testSetDelete() {
        Hashtable<String, Object> dt = new Hashtable<String, Object>();
        dt.put("speaks", new Vector<Object>());

        db.update(ptr, dt);
        Vector<Dictionary<String, Object>> result = db.executeQuery(speaksQuery, ptr);
        assertEquals(0, result.size());

        assertEquals(0, db.executeQuery("SELECT l FROM  test.Person__speaks l WHERE l.Person=$1", ptr).size());

        workWithSet(toInsert3);

        // clear the speaks set, so we can delete the languages w/o a foreign key error
        dt.put("speaks", new Vector<Object>());
        db.update(ptr, dt);
        db.delete("test.Language l", "1=1", null); // delete garbage
    }

    public void testPtrOneDelete() {
        db.delete(ptrOne);

        Dictionary<String, Object> d = db.read(ptr, personFields);
        assertNotNull(d);
        assertNull(d.get("extraData"));

        assertNull(db.read(ptrOne, ptrOneFields));
    }

    public void testPtrOneReInsert() {
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("extraData.something", "else2");
        db.update(ptr, p);
        Dictionary<String, Object> d = db.read(ptr, personFields);
        ptrOne = (Pointer) d.get("extraData");
        assertNotNull(ptrOne);
        Dictionary<String, Object> read;
        assertNotNull(read = db.read(ptrOne, ptrOneFields));
        assertEquals("else2", read.get("something"));
    }

    public void testUpdate() {
        Hashtable<String, Object> pmod = new Hashtable<String, Object>();
        String val = "A completely new guy";
        pmod.put("indiv.name", val);

        Vector<Integer> setintElem = CollectionUtils.toVector(2);
        pmod.put("intSet", setintElem);

        Vector<String> setcharElem = CollectionUtils.toVector("d");
        pmod.put("charSet", setcharElem);

        long before = System.currentTimeMillis();
        int updateReturn = db.update(ptr, pmod);
        long after = System.currentTimeMillis();

        Vector<Dictionary<String, Object>> v = db.executeQuery(readPerson, ptr);
        assertEquals(1, v.size());
        assertEquals(1, updateReturn);

        Dictionary<String, Object> modc = v.elementAt(0);

        assertNotNull(modc);
        assertEquals(val, modc.get("name"));
        assertEquals("doe", modc.get("surname"));

        Date modify = (Timestamp) modc.get("TS_modify");

        // should be < and > but ....
        // http://bugs.mysql.com/bug.php?id=8523
        assertTrue(before <= modify.getTime() + 1000);
        assertTrue(after >= modify.getTime() - 1000);

        assertNotNull(db.read(ptrOne, ptrOneFields));

        v = db.executeQuery(readIntSet, ptr);
        assertEquals(1, v.size());
        assertEquals(new Integer(2), v.elementAt(0).get("member"));

        v = db.executeQuery(readCharSet, ptr);
        assertEquals(1, v.size());
        assertEquals("d", v.elementAt(0).get("member"));
    }

    public void testUpdateWithDiff() {
        ArrayList<FieldValueDiff> expectedChanges = new ArrayList<FieldValueDiff>();
        Hashtable<String, Object> pmod = new Hashtable<String, Object>();

        DataDefinition dd = DataDefinitionProvider.getInstance().getDataDefinition(ptr.getType());

        String fieldName = "indiv.name";
        String newName = "Still the same guy";
        pmod.put(fieldName, newName);
        expectedChanges.add(new FieldValueDiff(fieldName, dd.getFieldOrPointedFieldDefinition(fieldName),
                "A completely new guy", newName));

        String fieldSurname = "indiv.surname";
        String newSurname = "D'oh";
        pmod.put(fieldSurname, newSurname);
        expectedChanges.add(new FieldValueDiff(fieldSurname, dd.getFieldOrPointedFieldDefinition(fieldSurname), "doe",
                newSurname));

        pmod.put("weight", new Double(85.7d));

        String fieldIntSet = "intSet";
        Vector<Integer> setintElem = CollectionUtils.toVector(1, 1);
        pmod.put(fieldIntSet, setintElem);
        expectedChanges.add(new FieldValueDiff(fieldIntSet, dd.getFieldOrPointedFieldDefinition(fieldIntSet),
                CollectionUtils.toVector(2), setintElem));

        String fieldCharSet = "charSet";
        Vector<String> setcharElem = CollectionUtils.toVector("e", "f");
        pmod.put(fieldCharSet, setcharElem);
        expectedChanges.add(new FieldValueDiff(fieldCharSet, dd.getFieldOrPointedFieldDefinition(fieldCharSet),
                CollectionUtils.toVector("d"), setcharElem));

        List<FieldValueDiff> res = db.updateWithValueDiff(ptr, pmod);

        Collections.sort(expectedChanges);
        Collections.sort(res);
        assertEquals(res, expectedChanges);

        Vector<Dictionary<String, Object>> v = db.executeQuery(readPerson, ptr);
        assertEquals(1, v.size());

        Dictionary<String, Object> modc = v.elementAt(0);

        assertNotNull(modc);
        assertEquals(newName, modc.get("name"));
        assertEquals(newSurname, modc.get("surname"));
        assertNotNull(db.read(ptrOne, ptrOneFields));

        v = db.executeQuery(readIntSet, ptr);
        assertEquals(setintElem.size(), v.size());
        assertEquals(setintElem, db.readIntEnumValues(ptr, fieldIntSet));

        v = db.executeQuery(readCharSet, ptr);
        assertEquals(setcharElem.size(), v.size());
        assertEquals(setcharElem, db.readIntEnumValues(ptr, fieldCharSet));
    }

    public void testDelete() {
        db.delete(ptr);

        assertNull(db.read(ptr, personFields));
        assertNull(db.read(ptrOne, ptrOneFields));
        assertEquals(0, db.executeQuery(subsetQuery, ptr).size());
        assertNull(db.read(set1, subsetFields));
        assertNull(db.read(set2, subsetFields));
        assertEquals(0, db.executeQuery(speaksQuery, ptr).size());
        assertEquals(0, db.executeQuery(readIntSet, ptr).size());
        assertEquals(0, db.executeQuery(readCharSet, ptr).size());
        assertEquals(0, db.executeQuery("SELECT l FROM  test.Person__speaks l WHERE l.Person=$1", ptr).size());
        assertEquals(0, db.executeQuery("SELECT l FROM  test.Person__intSet l WHERE l.Person=$1", ptr).size());
        assertEquals(0, db.executeQuery("SELECT l FROM  test.Person__charSet l WHERE l.Person=$1", ptr).size());

        // delete all entries, bug 673:
        deleteAllEntries();
    }

    protected void deleteAllEntries() {
        db.delete("test.validMdds.CharWithLength name", "name.name='bla'", null);
        db.delete("test.validMdds.CharWithLength t", "5=5", null);
        db.delete("test.validMdds.CharWithLength	t", "t.name LIKE \'www\'", null);
        db.delete("test.validMdds.CharWithLength bla", "'x'=bla.name", null);
    }

    public void testRealAggregation() {
        db.delete("test.validMdds.Real r", "r=r", null); // delete all
        // entries first
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("r", new Double(.5d));
        db.insert("test.validMdds.Real", p);
        p.put("r", new Double(.2d));
        db.insert("test.validMdds.Real", p);
        p.put("r", new Double(1.8d));
        db.insert("test.validMdds.Real", p);
        p.put("r", new Double(.0008d));
        db.insert("test.validMdds.Real", p);
        Vector<Dictionary<String, Object>> v = db.executeQuery(
            "SELECT avg(r.r) as av, sum(r.r) as su FROM  test.validMdds.Real r", null);
        assertEquals("Real aggregation", 1, v.size());
        assertEquals("Avg(reals)", new Double(0.6252d), v.firstElement().get("av"));
        assertEquals("Sum(reals)", new Double(2.5008d), v.firstElement().get("su"));

        Object[] args = { new Double(0.2), new Double(1.8) };
        v = db.executeQuery("SELECT r FROM  test.validMdds.Real r WHERE r.r>$1 AND r.r<=$2", args);
        assertEquals("Real comparisment", 2, v.size());

        // should we allow this? FIXME!
        Object[] args2 = { new Integer(-1), new Double(1.5) };
        v = db.executeQuery("SELECT count(r) as cnt FROM  test.validMdds.Real r WHERE r.r>$1 AND r.r<=$2", args2);
        assertEquals("Real comparison with integer parameter", new Integer(3), v.firstElement().get("cnt"));

        Object[] args3 = { new Double(1.5) };
        v = db.executeQuery("SELECT count(r) as cnt FROM  test.validMdds.Real r WHERE r.r>-1 AND r.r<=$1", args3);
        assertEquals("Real comparison with hardcoded integer", new Integer(3), v.firstElement().get("cnt"));

        db.delete("test.validMdds.Real r", "r=r", null); // delete garbage
    }

    public void testIntAggregation() {
        db.delete("test.validMdds.Int iii", "5=5", null); // delete all
        // entries first
        Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("i", new Integer(0));
        db.insert("test.validMdds.Int", p);
        p.put("i", new Integer(1));
        db.insert("test.validMdds.Int", p);
        p.put("i", new Integer(2));
        db.insert("test.validMdds.Int", p);
        p.put("i", new Integer(3));
        db.insert("test.validMdds.Int", p);
        p.put("i", new Integer(4));
        db.insert("test.validMdds.Int", p);
        p.put("i", new Integer(5));
        db.insert("test.validMdds.Int", p);
        Vector<Dictionary<String, Object>> v = db.executeQuery(
            "SELECT avg(i.i) as av, sum(i.i) as su FROM  test.validMdds.Int i", null);
        assertEquals("Int aggregation", 1, v.size());
        assertEquals("Avg(ints)", new Double(2.5d), v.firstElement().get("av"));
        assertEquals("Sum(ints)", new Integer(15), v.firstElement().get("su"));

        db.delete("test.validMdds.Int iii", "5=5", null); // delete garbage
    }

    public void testDeleteSetWithSet() {
        Hashtable<String, Object> personData = new Hashtable<String, Object>();
        personData.put("indiv.name", "rudi");
        personData.put("indiv.surname", "doe");
        Pointer ptrPerson = db.insert("test.Person", personData);

        Vector<Object> languages = new Vector<Object>();

        Hashtable<String, Object> language = new Hashtable<String, Object>();
        for (String[] element : languageData) {
            language.put("name", element[0]);
            language.put("isoCode", element[1]);
            languages.add(db.insert("test.Language", language));
        }

        Hashtable<String, Object> address = new Hashtable<String, Object>();
        address.put("streetno", "Sesame Street 15");
        address.put("languages", languages);
        address.put("sth.aaa", "someAAA");

        db.insert(ptrPerson, "address", address);
        address.put("streetno", "Sesame Street 16");
        address.put("sth.bbb", "someAAA");
        Pointer ptrAddress2 = db.insert(ptrPerson, "address", address);
        // check that we have two subsets
        assertEquals(2, db.executeQuery("SELECT a as a FROM test.Person p, p.address a WHERE p=$1", ptrPerson).size());
        // delete one including deleting subsets, and check that we have now 1 address, and still 45 languages
        db.delete(ptrAddress2);
        assertEquals(1, db.executeQuery("SELECT a as a FROM test.Person p, p.address a WHERE p=$1", ptrPerson).size());
        assertEquals(5, db.executeQuery("SELECT l as l FROM test.Language l", null).size());

        // delte the person (should delete all subPtrs/Sets), check that we still have 5 languages
        db.delete(ptrPerson);
        assertEquals(5, db.executeQuery("SELECT l as l FROM test.Language l", null).size());

        db.delete("test.Language l", "1=1", null); // delete garbage
    }

    public void testCopy() {
        Hashtable<String, Object> p = new Hashtable<String, Object>();
        p.put("birthdate", new java.util.GregorianCalendar(1977, 7, 7).getTime());
        p.put("indiv.name", "john");
        p.put("indiv.surname", "Copy");
        p.put("extraData.something", "else");

        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(1976, 2, 9);

        Date cr = c.getTime();
        p.put("TS_create", cr);

        c.clear();
        c.set(1976, 2, 10);

        Date mod = c.getTime();
        p.put("TS_modify", mod);

        ptr1 = db.insert("test.Person", p);
        assertNotNull(ptr1);

        Vector<Dictionary<String, Object>> v = db.executeQuery(readPerson, ptr1);
        assertEquals(1, v.size());

        pc1 = v.elementAt(0);
        assertNotNull(pc1);

        assertEquals("john", pc1.get("name"));
        assertEquals("Copy", pc1.get("surname"));
        assertEquals(cr, new Date(((Date) pc1.get("TS_create")).getTime()));
        assertEquals(mod, new Date(((Date) pc1.get("TS_modify")).getTime()));
        db.delete(ptr1);
        db.delete("test.Individual i", "1=1", null);
    }

    public void testInsertFromQuery() {
        Hashtable<String, Object> p = new Hashtable<String, Object>();
        p.put("name", "greek");
        Pointer ptr = db.insert("test.Language", p);
        assertNotNull(ptr);
        try {
            int n = db.insertFromQuery("test.Language",
                "SELECT ? as family, l.name as name FROM test.Language l WHERE l.name=?", new Object[] { 10, "greek" });
            assertEquals(n, 1);
        } catch (MakumbaError e) {
            // for hibernate
            assertEquals(e.getMessage(), "Not implemented");
            int m = db.delete("test.Language l", "1=1", null);
            assertEquals(m, 1);
            return;
        }
        Vector<Dictionary<String, Object>> v = db.executeQuery(
            "SELECT l.name as name, l.family as family FROM test.Language l ORDER BY l", null);

        int m = db.delete("test.Language l", "1=1", null);
        assertEquals(m, 2);

        assertEquals(v.size(), 2);
        assertNull(v.get(0).get("family"));
        assertEquals(10, v.get(1).get("family"));
    }

    private final MakumbaTestData testData = new MakumbaTestData();

    public void testMakTestData() {
        testData.insertLanguages(db);
        testData.insertPerson(db);
        int personNumber = db.executeQuery("SELECT p FROM test.Person p", null).size();
        int individualNumber = db.executeQuery("SELECT i FROM test.Individual i", null).size();
        testData.deletePersonsAndIndividuals(db);
        testData.deleteLanguages(db);

        // now we've done the cleanup so we can afford to fail
        assertEquals(2, personNumber);
        assertEquals(2, individualNumber);
    }
}
