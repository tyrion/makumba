/*
 * Created on Apr 29, 2005
 *
 */
package test.http;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;

import junit.extensions.TestSetup;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.cactus.JspTestCase;
import org.makumba.Database;
import org.makumba.MakumbaSystem;
import org.makumba.Pointer;
import org.makumba.Text;
import org.makumba.view.jsptaglib.MakumbaVersionTag;
import org.makumba.view.jsptaglib.QueryTag;

import com.meterware.httpunit.WebResponse;


/**
 * @author jpeeters
 */
public class TestTags extends JspTestCase {

	static Database db;
	static Pointer ptr;
	static Pointer set;
	static Dictionary pc;
	static Vector v;
	static String readPerson = "SELECT p.indiv.name AS name, p.indiv.surname AS surname, p.gender AS gender, p.uniqChar AS uniqChar, p.uniqInt AS uniqInt, p.birthdate AS birthdate, p.weight AS weight, p.TS_modify AS TS_modify, p.TS_create AS TS_create, p.comment AS comment, a.description AS description, a.email AS email, a.usagestart AS usagestart FROM test.Person p, p.address a WHERE p= $1";
	private String output;
	private String line;

	static Object[][] languageData = { { "English", "en" }, { "French", "fr" },
			{ "German", "de" }, { "Italian", "it" }, { "Spanish", "sp" } };
	
	public static Test suite() {
		TestSetup setup = new TestSetup(new TestSuite(TestTags.class)) {
			ArrayList ptrs = new ArrayList();
			protected void setUp() {
				// do your one-time setup here!
				db = MakumbaSystem.getConnectionTo(MakumbaSystem.getDefaultDatabaseName("test/testDatabase.properties"));
				insertPerson();
				insertLanguages();				
			}
			
			protected void insertPerson() {
				Properties p = new Properties();

				p.put("indiv.name", "john");
				
				Calendar c = Calendar.getInstance();
				c.clear();
				c.set(1977, 2, 5);
				Date birthdate = c.getTime();
				p.put("birthdate", birthdate);
						
				
				p.put("gender", new Integer(1));
				p.put("uniqChar", new String("testing \" character field"));
				
				p.put("weight", new Double(85.7d));
				
				p.put("comment", new Text("This is a text field. It's a comment about this person."));

				p.put("uniqInt", new Integer(255));
				
				Vector intSet = new Vector();
				intSet.addElement(new Integer(1));
				intSet.addElement(new Integer(0));
				p.put("intSet", intSet);

				ptr = db.insert("test.Person", p);
				
				p.clear();
				p.put("description", "");
				p.put("usagestart", birthdate);
				set=db.insert(ptr, "address", p);
				db.commit();
			}
			protected void deletePerson() {
				db.delete(set);
				db.delete(ptr);
			}
			
			protected void insertLanguages() {
				Dictionary p = new Hashtable();				
				for (int i = 0; i < languageData.length; i++) {
					p.put("name", languageData[i][0]);
					p.put("isoCode", languageData[i][1]);
					ptrs.add(db.insert("test.Language", p));
				}
				db.commit();
			}
			protected void deleteLanguages() {
				for (int i = 0; i<ptrs.size(); i++)
					db.delete((Pointer)ptrs.get(i));
			}
			
			protected void tearDown() {
				// do your one-time tear down here!
				deletePerson();
				deleteLanguages();
				db.close();
			}
		};
		return setup;
	}	
	
	public void testVersionTag() throws JspException, IOException {
		MakumbaVersionTag versionTag = new MakumbaVersionTag();
		versionTag.setPageContext(pageContext);
		versionTag.doStartTag();
		session.setAttribute("version", "0.0");
		Assert.assertEquals(1, versionTag.doStartTag());
		BodyContent bodyContent = pageContext.pushBody();
		bodyContent.println("Makumbaaaaaaaaaaa");
		bodyContent.print("Version 0");
		versionTag.doAfterBody();
		versionTag.doEndTag();
		pageContext.popBody();
	}

	public void testDataInput() throws JspException, JspException {
		assertNotNull(ptr);
		assertEquals(ptr.getType(), "test.Person");
	}
	
	public void testMakObjectTag() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakObjectTag.jsp");		
	}
	public void endMakObjectTag(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		
		v = db.executeQuery(readPerson, ptr);

		assertEquals(1, v.size());
		pc = (Dictionary) v.elementAt(0);

		output = output.substring(output.indexOf("name") + 5);
		assertEquals(pc.get("name"), output.substring(0, output.indexOf("\r")));
		output = output.substring(output.indexOf("weight") + 7);
		assertEquals(pc.get("weight").toString(), output.substring(0, output
				.indexOf("\r")));
	}

	public void testMakListTag() throws ServletException, IOException {
		QueryTag maklist = new QueryTag();
		pageContext.include("testMakListTag.jsp");
	}
	public void endMakListTag(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		int i = 0, begin, end;

		while ((begin = output.indexOf("name")) != -1) {
			output = output.substring(begin + 5);
			assertEquals(languageData[i][0], output.substring(0, output
					.indexOf("\r")));
			output = output.substring(output.indexOf("isoCode") + 8);
			assertEquals(languageData[i][1], output.substring(0, output
					.indexOf("\r")));
			i++;
		}
		try {
			assertEquals(true, response.getText().indexOf("English") != -1);
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		return;
	}

	public void testMakValueChar() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakValueChar.jsp");		
	}	
	public void endMakValueChar(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		v = db.executeQuery(readPerson, ptr);
		assertEquals("Database query empty", v.size(), 1);
		pc = (Dictionary) v.elementAt(0);


		assertTrue("testName! not found", output.indexOf("testName!") > 0 ? true : false);
		assertTrue("!endName not found", output.indexOf("!endName", output.indexOf("testName!")) > 0 ? true : false);
		assertEquals("jsp differs from database content", pc.get("name"), output.substring(output.indexOf("testName!")+9, output.indexOf("!endName")));
		
		assertTrue("testSurnameDefault! not found", output.indexOf("testSurnameDefault!") > 0 ? true : false);
		assertTrue("!endSurnameDefault not found", output.indexOf("!endSurnameDefault", output.indexOf("testSurnameDefault!")) > 0 ? true : false);
		assertEquals("default property not evaluated", "N/A", output.substring(output.indexOf("testSurnameDefault!")+19, output.indexOf("!endSurnameDefault")));		
		
		assertTrue("testUniqCharUrlEncode! not found", output.indexOf("testUniqCharUrlEncode!") > 0 ? true : false);
		assertTrue("!endUniqCharUrlEncode not found", output.indexOf("!endUniqCharUrlEncode", output.indexOf("testUniqCharUrlEncode!")) > 0 ? true : false);
		assertEquals("character string not url encoded", "testing+%26quot%3B+character+field", output.substring(output.indexOf("testUniqCharUrlEncode!")+22, output.indexOf("!endUniqCharUrlEncode")));		
		
		assertTrue("testUniqCharAuto! not found", output.indexOf("testUniqCharAuto!") > 0 ? true : false);
		assertTrue("!endUniqCharAuto not found", output.indexOf("!endUniqCharAuto", output.indexOf("testUniqCharAuto!")) > 0 ? true : false);
		assertEquals("automatic html transformation doesn't work", ((String)pc.get("uniqChar")).replaceAll("\"","&quot;"), output.substring(output.indexOf("testUniqCharAuto!")+17, output.indexOf("!endUniqCharAuto")));

		// TODO addTitle prints ' for "
		// assertEquals("<span title=\""+pc.get("uniqChar")+"\">"+((String)pc.get("uniqChar")).replaceAll("\"","&quot;")+"</span>", output.substring(output.indexOf("testUniqCharAutoAddTitleTrue!")+29, output.indexOf("!endUniqCharAutoAddTitleTrue")));		
		
		assertTrue("testUniqCharHtml! not found", output.indexOf("testUniqCharHtml!") > 0 ? true : false);
		assertTrue("!endUniqCharHtml not found", output.indexOf("!endUniqCharHtml", output.indexOf("testUniqCharHtml!")) > 0 ? true : false);
		assertEquals("content transformed to html, although we didn't ask for it", pc.get("uniqChar"), output.substring(output.indexOf("testUniqCharHtml!")+17, output.indexOf("!endUniqCharHtml")));
		
		assertTrue("testUniqCharNoHtml! not found", output.indexOf("testUniqCharNoHtml!") > 0 ? true : false);
		assertTrue("!endUniqCharNoHtml not found", output.indexOf("!endUniqCharNoHtml", output.indexOf("testUniqCharNoHtml!")) > 0 ? true : false);		
		assertEquals("html tranformation failed", ((String)pc.get("uniqChar")).replaceAll("\"","&quot;"), output.substring(output.indexOf("testUniqCharNoHtml!")+19, output.indexOf("!endUniqCharNoHtml")));
		
		assertTrue("testUniqCharMaxLength! not found", output.indexOf("testUniqCharMaxLength!") > 0 ? true : false);
		assertTrue("!endUniqCharMaxLength not found", output.indexOf("!endUniqCharMaxLength", output.indexOf("testUniqCharMaxLength!")) > 0 ? true : false);
		assertEquals("char not cut off or ellipsis not added", ((String)pc.get("uniqChar")).substring(0,5)+"...", output.substring(output.indexOf("testUniqCharMaxLength!")+22, output.indexOf("!endUniqCharMaxLength")));
		
		assertTrue("testUniqCharMaxLengthEllipsis! not found", output.indexOf("testUniqCharMaxLengthEllipsis!") > 0 ? true : false);
		assertTrue("!endUniqCharMaxLengthEllipsis not found", output.indexOf("!endUniqCharMaxLengthEllipsis", output.indexOf("testUniqCharMaxLengthEllipsis!")) > 0 ? true : false);
		assertEquals("ellipsis property not evaluated", ((String)pc.get("uniqChar")).substring(0,5)+"---", output.substring(output.indexOf("testUniqCharMaxLengthEllipsis!")+30, output.indexOf("!endUniqCharMaxLengthEllipsis")));
		
//		assertEquals("<span title=\""+pc.get("uniqChar")+"\">"+((String)pc.get("uniqChar")).substring(0,5)+"---</span>", output.substring(output.indexOf("testUniqCharMaxLengthEllipsisAddTitleAuto!")+42, output.indexOf("!endUniqCharMaxLengthEllipsisAddTitleAuto")));
		
		assertTrue("testExtraDataSomething! not found", output.indexOf("testExtraDataSomething!") > 0 ? true : false);
		assertTrue("!endExtraDataSomething not found", output.indexOf("!endExtraDataSomething", output.indexOf("testExtraDataSomething!")) > 0 ? true : false);
		assertEquals("problem with printing empty char", pc.get("description"), output.substring(output.indexOf("testExtraDataSomething!")+23, output.indexOf("!endExtraDataSomething")));

		assertTrue("testExtraDataSomethingEmpty! not found", output.indexOf("testExtraDataSomethingEmpty!") > 0 ? true : false);
		assertTrue("!endExtraDataSomethingEmpty not found", output.indexOf("!endExtraDataSomethingEmpty", output.indexOf("testExtraDataSomethingEmpty!")) > 0 ? true : false);
		assertEquals("empty property not evaluated", "N/A", output.substring(output.indexOf("testExtraDataSomethingEmpty!")+28, output.indexOf("!endExtraDataSomethingEmpty")));		
		
	}
	
	public void testMakValueDate() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakValueDate.jsp");		
	}	
	public void endMakValueDate(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		v = db.executeQuery(readPerson, ptr);
		assertEquals("Database query empty", v.size(), 1);
		pc = (Dictionary) v.elementAt(0);
		
		assertTrue("testBirthdate! not found", output.indexOf("testBirthdate!") > 0 ? true : false);
		assertTrue("!endBirthdate not found", output.indexOf("!endBirthdate", output.indexOf("testBirthdate!")) > 0 ? true : false);
		DateFormat df = new SimpleDateFormat("dd MMMM yyyy", MakumbaSystem.getLocale());
		assertEquals("jsp differs from database content", df.format(pc.get("birthdate")), output.substring(output.indexOf("testBirthdate!")+14, output.indexOf("!endBirthdate")));
		
		assertTrue("testBirthdateFormat! not found", output.indexOf("testBirthdateFormat!") > 0 ? true : false);
		assertTrue("!endBirthdateFormat not found", output.indexOf("!endBirthdateFormat", output.indexOf("testBirthdateFormat!")) > 0 ? true : false);
		df = new SimpleDateFormat("dd-mm-yy", MakumbaSystem.getLocale());
		assertEquals("date isn't formatted to the one specified", df.format(pc.get("birthdate")), output.substring(output.indexOf("testBirthdateFormat!")+20, output.indexOf("!endBirthdateFormat")));		
	}

	public void testMakValueInt() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakValueInt.jsp");	
	}
	public void endMakValueInt(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		v = db.executeQuery(readPerson, ptr);
		assertEquals("Database query empty", v.size(), 1);
		pc = (Dictionary) v.elementAt(0);
		
		assertTrue("testGender! not found", output.indexOf("testGender!") > 0 ? true : false);
		assertTrue("!endGender not found", output.indexOf("!endGender", output.indexOf("testGender")) > 0 ? true : false);
		assertEquals("jsp differs from database content", "Male", output.substring(output.indexOf("testGender!")+11, output.indexOf("!endGender")));
		
		assertTrue("testUniqInt! not found", output.indexOf("testUniqInt!") > 0 ? true : false);
		assertTrue("!endUniqInt not found", output.indexOf("!endUniqInt", output.indexOf("testUniqInt!")) > 0 ? true : false);
		assertEquals("jsp differs from database content", pc.get("uniqInt").toString(), output.substring(output.indexOf("testUniqInt!")+12, output.indexOf("!endUniqInt")));
	}
	
	public void testMakValueDouble() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakValueDouble.jsp");
	}
	public void endMakValueDouble(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		v = db.executeQuery(readPerson, ptr);
		assertEquals("Database query empty", v.size(), 1);
		pc = (Dictionary) v.elementAt(0);
		
		assertTrue("testWeight! not found", output.indexOf("testWeight!") > 0 ? true : false);
		assertTrue("!endWeight not found", output.indexOf("!endWeight", output.indexOf("testWeight!")) > 0 ? true : false);
		assertEquals("jsp differs from database content", pc.get("weight").toString(), output.substring(output.indexOf("testWeight!")+11, output.indexOf("!endWeight")));
	}
	
	public void testMakValueText() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakValueText.jsp");
	}
	public void endMakValueText(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		v = db.executeQuery(readPerson, ptr);
		assertEquals("Database query empty", v.size(), 1);
		pc = (Dictionary) v.elementAt(0);

		assertTrue("testComment! not found", output.indexOf("testComment!") > 0 ? true : false);
		assertTrue("!endComment not found", output.indexOf("!endComment", output.indexOf("testComment!")) > 0 ? true : false);
		assertEquals("default lineseperator not added", "<p>"+pc.get("comment")+"</p>", output.substring(output.indexOf("testComment!")+12, output.indexOf("!endComment")));
		
		assertTrue("testCommentLineSeparator! not found", output.indexOf("testCommentLineSeparator!") > 0 ? true : false);
		assertTrue("!endCommentLineSeparator not found", output.indexOf("!endCommentLineSeparator", output.indexOf("testCommentLineSeparator!")) > 0 ? true : false);
		assertEquals("custom lineseperator not added", "<abc>"+pc.get("comment"), output.substring(output.indexOf("testCommentLineSeparator!")+25, output.indexOf("!endCommentLineSeparator")));
		
		assertTrue("testCommentLongLineLength! not found", output.indexOf("testCommentLongLineLength!") > 0 ? true : false);
		assertTrue("!endCommentLongLineLength not found", output.indexOf("!endCommentLongLineLength", output.indexOf("testCommentLongLineLength!")) > 0 ? true : false);
		assertEquals("longLineLength not parsed", pc.get("comment").toString(), output.substring(output.indexOf("testCommentLongLineLength!")+26, output.indexOf("!endCommentLongLineLength")));
	}
	
	public void testMakValueSet() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakValueSet.jsp");
	}
	public void endMakValueSet(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		v = db.executeQuery(readPerson, ptr);
		assertEquals("Database query empty", v.size(), 1);
		pc = (Dictionary) v.elementAt(0);

		assertTrue("testAddressDescription! not found", output.indexOf("testAddressDescription!") > 0 ? true : false);
		assertTrue("!endAddressDescription not found", output.indexOf("!endAddressDescription", output.indexOf("testAddressDescription!")) > 0 ? true : false);
		assertEquals("problem with empty field", pc.get("description"), output.substring(output.indexOf("testAddressDescription!")+23, output.indexOf("!endAddressDescription")));
		
		assertTrue("testAddressDescriptionEmpty! not found", output.indexOf("testAddressDescriptionEmpty!") > 0 ? true : false);
		assertTrue("!endAddressDescriptionEmpty not found", output.indexOf("!endAddressDescriptionEmpty", output.indexOf("testAddressDescriptionEmpty!")) > 0 ? true : false);
		assertEquals("problem with custom value for empty field", "N/A", output.substring(output.indexOf("testAddressDescriptionEmpty!")+28, output.indexOf("!endAddressDescriptionEmpty")));
		
		assertTrue("testAddressEmailDefault! not found", output.indexOf("testAddressEmailDefault!") > 0 ? true : false);
		assertTrue("!endAddressEmailDefault not found", output.indexOf("!endAddressEmailDefault", output.indexOf("testAddressEmailDefault!")) > 0 ? true : false);
		assertEquals("problem with default value for null field", "N/A", output.substring(output.indexOf("testAddressEmailDefault!")+24, output.indexOf("!endAddressEmailDefault")));
		
		assertTrue("testAddressUsagestart! not found", output.indexOf("testAddressUsagestart!") > 0 ? true : false);
		assertTrue("!endAddressUsagestart not found", output.indexOf("!endAddressUsagestart", output.indexOf("testAddressUsagestart!")) > 0 ? true : false);
		DateFormat df = new SimpleDateFormat("dd MMMM yyyy", MakumbaSystem.getLocale());
		assertEquals("jsp differs from database content", df.format(pc.get("usagestart")), output.substring(output.indexOf("testAddressUsagestart!")+22, output.indexOf("!endAddressUsagestart")));
	}
	
	public void testMakValueTS_create() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakValueTS_create.jsp");		
	}
	public void endMakValueTS_create(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		v = db.executeQuery(readPerson, ptr);
		assertEquals("Database query empty", v.size(), 1);
		pc = (Dictionary) v.elementAt(0);

		assertTrue("testTS_create! not found", output.indexOf("testTS_create!") > 0 ? true : false);
		assertTrue("!endTS_create not found", output.indexOf("!endTS_create", output.indexOf("testTS_create!")) > 0 ? true : false);
		assertEquals("TS_create value not correct", pc.get("TS_create").toString(), output.substring(output.indexOf("testTS_create!")+14, output.indexOf("!endTS_create")));
	}
	
	public void testMakValueTS_modify() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakValueTS_modify.jsp");
	}
	public void endMakValueTS_modify(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		v = db.executeQuery(readPerson, ptr);
		assertEquals("Database query empty", v.size(), 1);
		pc = (Dictionary) v.elementAt(0);

		assertTrue("testTS_modify! not found", output.indexOf("testTS_modify!") > 0 ? true : false);
		assertTrue("!endTS_modify not found", output.indexOf("!endTS_modify", output.indexOf("testTS_modify!")) > 0 ? true : false);
		assertEquals("TS_modify value not correct", pc.get("TS_modify").toString(), output.substring(output.indexOf("testTS_modify!")+14, output.indexOf("!endTS_modify")));
	}

	protected String removeNewlines(String line) {
		String parsed = line;
		parsed = parsed.replaceAll("[\n\r]*", "");
		return parsed;
	}
	protected String removeMakumbaResponder(String line) {		
		String parsed = line;		
		parsed = parsed.replaceAll("value=\"-?[0-9]*\"", "");
		return parsed;
	}
	protected String removeMakumbaBase(String line) {		
		String parsed = line;		
		parsed = parsed.replaceAll("value=\"[0-9a-zA-Z]*\"", "");
		return parsed;
	}
	
	public void testMakNewForm() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakNewForm.jsp");
	}
	public void endMakNewForm(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		
		assertTrue("testNewFormStart! not found", output.indexOf("testNewFormStart!") > 0 ? true : false);
		assertTrue("!endNewFormStart not found", output.indexOf("!endNewFormStart", output.indexOf("testNewFormStart!")) > 0 ? true : false);
		assertEquals("form start incorrect", "<form action=\"testMakNewForm.jsp\" method=\"post\">", output.substring(output.indexOf("testNewFormStart!")+17, output.indexOf("!endNewFormStart")));
		
		assertTrue("testNewFormEnd! not found", output.indexOf("testNewFormEnd!") > 0 ? true : false);
		assertTrue("!endNewFormEnd not found", output.indexOf("!endNewFormEnd", output.indexOf("testNewFormEnd!")) > 0 ? true : false);
		line = output.substring(output.indexOf("testNewFormEnd!")+15, output.indexOf("!endNewFormEnd")).trim();
		assertTrue("__makumba__responder__ not found", line.indexOf("__makumba__responder__") > 0 ? true : false);
		line = removeNewlines(line);
		line = removeMakumbaResponder(line);
		assertEquals("form end incorrect", "<input type=\"hidden\" name=\"__makumba__responder__\" ></form>", line);
		
		assertTrue("testName! not found", output.indexOf("testName!") > 0 ? true : false);
		assertTrue("!endName not found", output.indexOf("!endName", output.indexOf("testName!")) > 0 ? true : false);
		assertEquals("problem with input field ", "<input name=\"indiv.name\" type=\"text\" value=\"\" maxlength=\"40\" >", output.substring(output.indexOf("testName!")+9, output.indexOf("!endName")));
		
		assertTrue("testSubmit! not found", output.indexOf("testSubmit!") > 0 ? true : false);
		assertTrue("!endSubmit not found", output.indexOf("!endSubmit", output.indexOf("testSubmit!")) > 0 ? true : false);
		assertEquals("problem with submit button", "<input type=\"submit\">", output.substring(output.indexOf("testSubmit!")+11, output.indexOf("!endSubmit")));
		
	}

/*	public void testMakAddForm() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakAddForm.jsp");
	}
	public void endMakAddForm(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		
		
	}*/
	
	public void testMakEditForm() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakEditForm.jsp");
	}
	public void endMakEditForm(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		
		assertTrue("testMakEditFormStart! not found", output.indexOf("testMakEditFormStart!") > 0 ? true : false);
		assertTrue("!endMakEditFormStart not found", output.indexOf("!endMakEditFormStart", output.indexOf("testMakEditFormStart")) > 0 ? true : false);
		assertEquals("form start incorrect", "<form action=\"testMakEditForm.jsp\" method=\"post\">", output.substring(output.indexOf("testMakEditFormStart!")+21, output.indexOf("!endMakEditFormStart")));
		
		assertTrue("testMakEditFormEnd! not found", output.indexOf("testMakEditFormEnd!") > 0 ? true : false);
		assertTrue("!endMakEditFormEnd not found", output.indexOf("!endMakEditFormEnd", output.indexOf("testMakEditFormEnd!")) > 0 ? true : false);
		line = output.substring(output.indexOf("testMakEditFormEnd!")+19, output.indexOf("!endMakEditFormEnd")).trim();
		assertTrue("__makumba__responder__ not found", line.indexOf("__makumba__responder__") > 0 ? true : false);
		assertTrue("__makumba__base__ not found", line.indexOf("__makumba__base__") > 0 ? true : false);
		line = removeNewlines(line);
		line = removeMakumbaResponder(line);
		line = removeMakumbaBase(line);
		assertEquals("form end incorrect", "<input type=\"hidden\" name=\"__makumba__base__\" ><input type=\"hidden\" name=\"__makumba__responder__\" ></form>", line);
		
	}
	
	public void testMakForm() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakForm.jsp");
	}
	public void endMakForm(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}
		
		assertTrue("testMakFormStart! not found", output.indexOf("testMakFormStart!") > 0 ? true : false);
		assertTrue("!endMakFormStart not found", output.indexOf("!endMakFormStart", output.indexOf("testMakFormStart")) > 0 ? true : false);
		assertEquals("form start incorrect", "<form action=\"testMakAddForm.jsp\" method=\"post\">", output.substring(output.indexOf("testMakFormStart!")+17, output.indexOf("!endMakFormStart")));
		
		assertTrue("testMakFormEnd! not found", output.indexOf("testMakFormEnd!") > 0 ? true : false);
		assertTrue("!endMakFormEnd not found", output.indexOf("!endMakFormEnd", output.indexOf("testMakFormEnd!")) > 0 ? true : false);
		line = output.substring(output.indexOf("testMakFormEnd!")+15, output.indexOf("!endMakFormEnd")).trim();
		assertTrue("__makumba__responder__ not found", line.indexOf("__makumba__responder__") > 0 ? true : false);
		line = removeNewlines(line);
		line = removeMakumbaResponder(line);
		assertEquals("form end incorrect", "<input type=\"hidden\" name=\"__makumba__responder__\" ></form>", line);
		
		assertTrue("testInput! not found", output.indexOf("testInput!") > 0 ? true : false);
		assertTrue("!endInput not found", output.indexOf("!endInput", output.indexOf("testInput")) > 0 ? true : false);
		assertEquals("error in parsing input field", "<input name=\"xxx\" type=\"text\" value=\"john\" maxlength=\"40\" >", output.substring(output.indexOf("testInput!")+10, output.indexOf("!endInput")));
		
	}
	
	protected String removeLabelStuff(String line) {
		String parsed = line;
		parsed = parsed.replaceAll("id=\"AutoLabel_[0-9]*\"", "");
		parsed = parsed.replaceAll("<LABEL for=\"AutoLabel_[0-9]*\">", "");
		parsed = parsed.replaceAll("</LABEL>", "");
		return parsed;		
	}
	public void testMakInput() throws ServletException, IOException {
		QueryTag makobject = new QueryTag();
		pageContext.include("testMakInput.jsp");	
	}
	public void endMakInput(WebResponse response) {
		try {
			output = response.getText();
		} catch (IOException e) {
			fail("JSP output error: " + response.getResponseMessage());
		}

		assertTrue("editName! not found", output.indexOf("editName!") > 0 ? true : false);
		assertTrue("!endName not found", output.indexOf("!endName", output.indexOf("editName!")) > 0 ? true : false);
		assertEquals("char edit field not correct", "<input name=\"indiv.name\" type=\"text\" value=\"john\" maxlength=\"40\" >", output.substring(output.indexOf("editName!")+9, output.indexOf("!endName")));
		
		assertTrue("editSurname! not found", output.indexOf("editSurname!") > 0 ? true : false);
		assertTrue("!endSurname not found", output.indexOf("!endSurname", output.indexOf("editSurname!")) > 0 ? true : false);
		assertEquals("options for input field not parsed", "<input name=\"indiv.surname\" type=\"password\" value=\"\" maxlength=\"5\" >", output.substring(output.indexOf("editSurname!")+12, output.indexOf("!endSurname")));
		
		assertTrue("editGender! not found", output.indexOf("editGender!") > 0 ? true : false);
		assertTrue("!endGender not found", output.indexOf("!endGender", output.indexOf("editGender!")) > 0 ? true : false);				
		assertEquals("problem with radio buttons", "<INPUT TYPE=RADIO NAME=\"gender\"  VALUE=\"0\"  > Female <INPUT TYPE=RADIO NAME=\"gender\"  VALUE=\"1\" CHECKED  > Male", removeLabelStuff(output.substring(output.indexOf("editGender!")+11, output.indexOf("!endGender"))));

		assertTrue("editBirthdate! not found", output.indexOf("editBirthdate!") > 0 ? true : false);
		assertTrue("!endBirthdate not found", output.indexOf("!endBirthdate", output.indexOf("editBirthdate!")) > 0 ? true : false);
		assertEquals("problem with radio buttons", "<input type=\"text\" name=\"birthdate_2\" value=\"1977\" maxlength=\"4\" size=\"4\">-<select name=\"birthdate_1\"><option value=\"0\">01</option><option value=\"1\">02</option><option value=\"2\" selected>03</option><option value=\"3\">04</option><option value=\"4\">05</option><option value=\"5\">06</option><option value=\"6\">07</option><option value=\"7\">08</option><option value=\"8\">09</option><option value=\"9\">10</option><option value=\"10\">11</option><option value=\"11\">12</option></select>-<select name=\"birthdate_0\"><option value=\"1\">01</option><option value=\"2\">02</option><option value=\"3\">03</option><option value=\"4\">04</option><option value=\"5\" selected>05</option><option value=\"6\">06</option><option value=\"7\">07</option><option value=\"8\">08</option><option value=\"9\">09</option><option value=\"10\">10</option><option value=\"11\">11</option><option value=\"12\">12</option><option value=\"13\">13</option><option value=\"14\">14</option><option value=\"15\">15</option><option value=\"16\">16</option><option value=\"17\">17</option><option value=\"18\">18</option><option value=\"19\">19</option><option value=\"20\">20</option><option value=\"21\">21</option><option value=\"22\">22</option><option value=\"23\">23</option><option value=\"24\">24</option><option value=\"25\">25</option><option value=\"26\">26</option><option value=\"27\">27</option><option value=\"28\">28</option><option value=\"29\">29</option><option value=\"30\">30</option><option value=\"31\">31</option></select>", output.substring(output.indexOf("editBirthdate!")+14, output.indexOf("!endBirthdate")));
		
		assertTrue("editComment! not found", output.indexOf("editComment!") > 0 ? true : false);
		assertTrue("!endComment not found", output.indexOf("!endComment", output.indexOf("editComment!")) > 0 ? true : false);
		assertEquals("options for input field not parsed", "<TEXTAREA name=\"comment\"  >This is a text field. It's a comment about this person.</TEXTAREA>", output.substring(output.indexOf("editComment!")+12, output.indexOf("!endComment")));
		
	}
}
