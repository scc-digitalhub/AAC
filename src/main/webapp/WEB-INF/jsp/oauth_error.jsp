<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <title>AAC</title>
  <link type="text/css" rel="stylesheet" href="<c:url value="../css/style.css"/>"/>
</head>

<body>

  <h1>AAC OAuth2 Error</h1>

  <div id="content">
	<p><c:out value="${message}"/> (<c:out value="${error.summary}"/>)</p>
	<p>Please go back to your client application and try again, or contact the administrator for support</p>
  </div>

</body>
</html>
