<%@page contentType="text/html"%>
<%@page pageEncoding="utf-8"%>
<html>
<head><title>Field types</title></head>
<body>

<%@taglib uri="http://www.makumba.org/presentation" prefix="mak" %>

<mak:object from="test.Person p, p.indiv i" where="i.name='john'">
    testMakEditFormStart!<mak:editForm object="p" action="testMakEditForm.jsp" method="post">!endMakEditFormStart
		<mak:input name="indiv.name" />
    testMakEditFormEnd!</mak:editForm>!endMakEditFormEnd
</mak:object>

</body>
</html>