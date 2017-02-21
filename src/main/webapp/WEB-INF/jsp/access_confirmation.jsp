<%@page import="org.springframework.security.web.WebAttributes"%>
<%@ page import="org.springframework.security.core.AuthenticationException" %>
<%@ page import="org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter" %>
<%@ page import="org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="resources.internal" var="res"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <title>AAC</title>
  <link type="text/css" rel="stylesheet" href="<c:url value="../css/style.css"/>"/>
  <link href="../css/bootstrap.min.css" rel="stylesheet" />
</head>

<body>
    <img class="logo" src="../img/ls_logo.png" alt="SmartCommunity" />
    <div class="clear"></div>

  <div class="confirmation-content container-fluid">
    <div class="row">
      <div class="col-md-offset-4 col-md-4">

    <% if (session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) != null && !(session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) instanceof UnapprovedClientAuthenticationException)) { %>
      <div class="error">
        <h2><fmt:message bundle="${res}" key="lbl_approval_error_title" /></h2>

        <p><fmt:message bundle="${res}" key="lbl_approval_error_message" />: (<%= ((AuthenticationException) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)).getMessage() %>)</p>
      </div>
    <% } %>
    <c:remove scope="session" var="SPRING_SECURITY_LAST_EXCEPTION"/>

      <h2>Please Confirm</h2>

      <p><fmt:message bundle="${res}" key="lbl_approval_authorize"><fmt:param value="${clientName}"/></fmt:message>:</p>
      <ul class="permission">
      <c:forEach items="${resources}" var="r">
        <li><c:out value="${r.name}"/></li>
      </c:forEach>        
      </ul>

      <div class="row">
        <div class="col-xs-3 col-xs-offset-3 text-right">
	      <form  id="confirmationForm" name="confirmationForm" action="<%=request.getContextPath()%>/oauth/authorize" method="post">
	        <input name="user_oauth_approval" value="true" type="hidden"/>
	        <label><input class="btn btn-primary" name="authorize" value="<fmt:message bundle="${res}" key="lbl_approval_do_authorize" />" type="submit"/></label>
	      </form>
        </div>
        <div class="col-xs-3">
	      <form id="denialForm" name="denialForm" action="<%=request.getContextPath()%>/oauth/authorize" method="post">
	        <input name="user_oauth_approval" value="false" type="hidden"/>
	        <label><input class="btn" name="deny" value="<fmt:message bundle="${res}" key="lbl_approval_do_deny" />" type="submit"/></label>
	      </form>
        </div>
      </div>
      </div>
    </div>
  </div>

</body>
</html>
