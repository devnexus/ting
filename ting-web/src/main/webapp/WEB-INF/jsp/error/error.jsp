<%@ page language="java" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="ctx" value="${pageContext['request'].contextPath}"/>

<!DOCTYPE html
        PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html>
  <head>
    <title>Error</title>

    <meta http-equiv="Cache-Control" content="no-store" />
    <meta http-equiv="Pragma"        content="no-cache" />
    <meta http-equiv="Expires"       content="0" />
    <meta http-equiv="content-type"  content="text/html; charset=utf-8" />

    <meta name="author"      content="Gunnar Hillert" />
    <meta name="keywords"    content="" />
    <meta name="description" content="" />

    <link rel="alternate"     href="<c:url value='/rss/devnexus.rss'/>" type="application/rss+xml" title="DevNexus RSS Feed" />
    <link rel="icon"          href="<c:url value='/favicon.ico'/>"  type="image/x-icon" />
    <link rel="shortcut icon" href="<c:url value='/favicon.ico'/>"  type="image/x-icon" />

        <script type="text/javascript">
        jQuery(init());

        jQuery(function() {
            jQuery(':input').bind('focus', function(event) { jQuery(event.target).addClass('selected'); });
            jQuery(':input').bind('blur', function(event) { jQuery(event.target).removeClass('selected'); });
        });
    </script>
  </head>
  <body>
    <div class="container"><div class="outer-header">
      <div class="header"><span class="ajug">AJUG</span> <span class="separator">|</span> Jobs</div></div>
      <div class="header_menu">
          <ul><li><a href="<c:url value='/'/>">HOME</a></li>
            <li style="margin-right: 1em; float: right;padding: 0.2em 0em;">
            <c:if test="${pageContext.request.secure}">Site is SSL secured</c:if></li>
          </ul>
      </div>
      <div class="content" style="overflow: auto;">

        An Error has occurred in this application.

    <%
     org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Application");
     LOGGER.error("Error JSP Page triggered!", exception);
    %>

      </div>
      <div class="footer"><a class="footerLogo"
        href="http://www.devnexus.org" title="DevNexus Conference">DevNexus</a>
      </div>
    </div>

            <script type="text/javascript">

            $(document).ready(function () {
            $('.outer-header').add_layer("url('${ctx}/images/icons/beta_2.0_badge.png') no-repeat 90% 50%");
            });
            </script>
  </body>
</html>
