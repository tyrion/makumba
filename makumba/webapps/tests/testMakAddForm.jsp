<%@page contentType="text/html"%>
<%@page pageEncoding="utf-8"%>
<html>
<head><title>Field types</title></head>
<body>

<%@taglib uri="http://www.makumba.org/presentation" prefix="mak" %>

<mak:object from="test.Person p, p.indiv i" where="i.name='john'">
    <mak:addForm object="p" field="speaks"
    		 action="testMakAddForm.jsp" method="post">
    </mak:addForm>
</mak:object>

</body>
</html>