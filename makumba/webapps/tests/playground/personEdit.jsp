<% /* $Id: /usr/local/cvsroot/karamba/public_html/welcome.jsp,v 2.39 2007/06/28 17:18:22 manu Exp $ */ %>
<jsp:include page="header.jsp" flush="false" />
<%@taglib uri="http://www.makumba.org/presentation" prefix="mak" %>


<mak:object from="test.Person p" where="p=$person">
<h1>Edit Person <i><mak:value expr="p.indiv.name" /></i></h1>
<mak:editForm object="p" action="personList.jsp">
<table>
  <tr><th>indiv.name</th><td><mak:input name="indiv.name" /></td></tr>  
  <tr><th>indiv.surname</th><td><mak:input name="indiv.surname" /></td></tr>  
  <tr><th>age</th><td><mak:input name="age" /></td></tr>  
  <tr><th>weight</th><td><mak:input name="weight" /></td></tr>  
  <tr><th>email</th><td><mak:input name="email" /></td></tr>  
  <tr><th>hobbies</th><td><mak:input name="hobbies" /></td></tr>  
  <tr><th>firstSex</th><td><mak:input name="firstSex" /></td></tr>  
  <tr><th>birthdate</th><td><mak:input name="birthdate" format="dd MMM yyyy HH:mm:ss"  /></td></tr>  
  <tr><th>beginDate</th><td><mak:input name="beginDate" /></td></tr> 
  <tr><th>uniqPtr</th><td><mak:input name="uniqPtr" /></td></tr> 
  <tr><th>uniqDate</th><td><mak:input name="uniqDate" format="dd MMM yyyy HH:mm:ss" /></td></tr> 
  <tr><th>uniqInt</th><td><mak:input name="uniqInt" /></td></tr> 
  <tr><th>uniqChar</th><td><mak:input name="uniqChar" /></td></tr> 
  <tr>
    <td colspan="2" align="center"><input type="submit" /> </td>
  </tr>
</table>  
</mak:editForm>
</mak:object>
</body>
</html>
