<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
      
<wiki:UserCheck status="authenticated"> <%-- changed by rudi: only show to authenticated users --%>
<div id="actionsTop" class="pageactions"> 
  <ul>

    <wiki:CheckRequestContext context='view|info|diff|upload|rename'>
    <wiki:Permission permission="edit">
	<li>
        <wiki:PageType type="page">
          <a href="<wiki:EditLink format='url' />" accesskey="e"  class="action edit"
            title="<fmt:message key='actions.edit.title'/>" ><fmt:message key='actions.edit'/></a>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <a href="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />" accesskey="e" class="action edit"
            title="<fmt:message key='actions.editparent.title'/>" ><fmt:message key='actions.editparent'/></a>
        </wiki:PageType>
    </li>
    </wiki:Permission>
    </wiki:CheckRequestContext>

    <%-- converted to popup menu by jspwiki-common.js--%>
    <li id="morebutton">
      <a href="<wiki:Link format='url' page='MoreMenu' />" class="action more"><fmt:message key="actions.more"/></a>
    </li>

  </ul>
</div>
</wiki:UserCheck>  <%-- changed by rudi: only show to authenticated users --%>

<wiki:UserCheck status="notAuthenticated"> <%-- changed by rudi: to non-authenticated users, just show a "comment" link --%>
  <wiki:Permission permission="comment">
    <wiki:PageType type="page">
      <a class="action comment" 
               href="<wiki:CommentLink format='url' />" 
              title="<fmt:message key='actions.comment.title' />"><fmt:message key="actions.comment" />
      </a>
    </wiki:PageType>
  </wiki:Permission>
</wiki:UserCheck>
