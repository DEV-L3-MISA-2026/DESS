<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="message" value="${requestScope['jakarta.servlet.error.message']}"/>
<c:choose>
    <c:when test="${status == 403}">
        <c:set var="title" value="Access denied"/>
        <c:set var="description" value="Your account does not have access to this area."/>
    </c:when>
    <c:when test="${status == 404}">
        <c:set var="title" value="Page not found"/>
        <c:set var="description" value="The page you requested does not exist."/>
    </c:when>
    <c:otherwise>
        <c:set var="title" value="Something went wrong"/>
        <c:set var="description" value="The request could not be completed."/>
    </c:otherwise>
</c:choose>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${title}"/> · DESS</title>
    <link rel="stylesheet" href="${ctx}/assets/css/tokens.css">
    <link rel="stylesheet" href="${ctx}/assets/css/layout.css">
    <link rel="stylesheet" href="${ctx}/assets/css/components.css">
    <link rel="stylesheet" href="${ctx}/assets/css/pages.css">
</head>
<body class="auth-body">
<main class="auth-main" id="main-content">
    <section class="auth-card" aria-labelledby="error-title">
        <div class="auth-brand">
            <span class="dess-brand-mark" aria-hidden="true">D</span>
            <div>
                <p class="auth-brand-name">DESS</p>
                <p class="auth-brand-sub">Department Exam and Schedule System</p>
            </div>
        </div>

        <p class="error-code">Status <c:out value="${status}"/></p>
        <h1 class="auth-title" id="error-title"><c:out value="${title}"/></h1>
        <p class="auth-description"><c:out value="${description}"/></p>

        <c:if test="${not empty message and status != 500}">
            <p class="auth-note"><c:out value="${message}"/></p>
        </c:if>

        <div class="form-actions">
            <a class="btn" href="${ctx}/app">Back to overview</a>
            <a class="btn btn-ghost" href="${ctx}/login">Sign in</a>
        </div>
    </section>
</main>
</body>
</html>
