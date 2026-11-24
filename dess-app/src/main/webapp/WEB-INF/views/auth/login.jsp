<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in · DESS</title>
    <link rel="icon" href="${ctx}/favicon.ico" sizes="any">
    <link rel="icon" type="image/png" href="${ctx}/assets/img/app-icon-192.png">
    <link rel="apple-touch-icon" href="${ctx}/assets/img/app-icon-192.png">
    <link rel="stylesheet" href="${ctx}/assets/css/tokens.css">
    <link rel="stylesheet" href="${ctx}/assets/css/layout.css">
    <link rel="stylesheet" href="${ctx}/assets/css/components.css">
    <link rel="stylesheet" href="${ctx}/assets/css/pages.css">
</head>
<body class="auth-body">
<main class="auth-main" id="main-content">
    <section class="auth-card" aria-labelledby="login-title">
        <div class="auth-brand">
            <span class="dess-brand-mark" aria-hidden="true">D</span>
            <div>
                <p class="auth-brand-name">DESS</p>
                <p class="auth-brand-sub">Department Exam and Schedule System</p>
            </div>
        </div>

        <h1 class="auth-title" id="login-title">Sign in</h1>
        <p class="auth-description">
            Use the account provided by your administrator.
        </p>

        <c:if test="${logout}">
            <div class="flash info" role="status">You have been signed out.</div>
        </c:if>

        <c:if test="${not empty error}">
            <div class="flash error" role="alert"><c:out value="${error}"/></div>
        </c:if>

        <c:if test="${noAccounts}">
            <div class="flash warn" role="status">
                No account exists yet. Set <span class="mono">DESS_ADMIN_PASSWORD</span>
                in the server environment and restart the application.
            </div>
        </c:if>

        <form method="post" action="${ctx}/login" class="stack">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="next" value="<c:out value='${next}'/>">

            <div class="field">
                <label for="username">Username</label>
                <input id="username" name="username" type="text" required autofocus
                       autocomplete="username" value="<c:out value='${username}'/>">
            </div>

            <div class="field">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" required
                       autocomplete="current-password">
            </div>

            <div class="form-actions">
                <button class="btn auth-submit" type="submit">Sign in</button>
            </div>
        </form>

        <p class="auth-note">
            Sessions expire after 30 minutes. Repeated failures lock the account for five minutes.
        </p>
    </section>
</main>
</body>
</html>
