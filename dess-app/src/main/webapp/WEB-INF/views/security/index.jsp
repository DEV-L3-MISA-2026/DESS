<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Security"/>
<c:set var="topbarContext" value="Accounts and roles"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="pageDescription" value="Sign-in accounts and their roles. Accounts are separate from people records, and password values and hashes are never displayed."/>
<c:set var="breadcrumbParentLabel" value="Overview"/>
<c:set var="breadcrumbParentHref" value="${ctx}/app"/>
<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="accounts-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="accounts-title">Accounts</h2>
            <p class="panel-note"><c:out value="${fn:length(accounts)}"/>
                <c:out value="${fn:length(accounts) == 1 ? 'account' : 'accounts'}"/>.</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty accounts}">
            <c:set var="emptyTitle" value="No account exists."/>
            <c:set var="emptyMessage" value="Set DESS_ADMIN_PASSWORD before starting the application to seed the administrator account, or create one below."/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="User accounts">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Username</th>
                        <th scope="col">Display name</th>
                        <th scope="col">Roles</th>
                        <th scope="col">Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="account" items="${accounts}">
                        <tr>
                            <td class="mono"><c:out value="${account.username}"/></td>
                            <td><c:out value="${account.displayName}"/></td>
                            <td><c:out value="${account.roles}"/></td>
                            <td>
                                <c:set var="badgeTone" value="${account.active ? 'ok' : 'off'}"/>
                                <c:set var="badgeLabel" value="${account.active ? 'Active' : 'Inactive'}"/>
                                <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<section class="panel" aria-labelledby="create-account-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="create-account-title">Create an account</h2>
            <p class="panel-note">The password is hashed on the server and is never shown again.</p>
        </div>
    </div>

    <c:if test="${not empty errors.form}">
        <div class="flash error" role="alert"><c:out value="${errors.form}"/></div>
    </c:if>
    <c:set var="validationErrors" value="${errors}"/>
    <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>

    <form method="post" action="${ctx}/app/security" class="stack">
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <input type="hidden" name="action" value="createAccount">

        <div class="field-grid">
            <div class="field">
                <label for="username">Username <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="username" name="username" type="text" required autocomplete="off"
                       value="<c:out value='${form.username}'/>"
                       <c:if test="${not empty errors.username}">aria-invalid="true" aria-describedby="username-error"</c:if>>
                <c:if test="${not empty errors.username}">
                    <p class="field-error" id="username-error"><c:out value="${errors.username}"/></p>
                </c:if>
            </div>

            <div class="field">
                <label for="displayName">Display name <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="displayName" name="displayName" type="text" required
                       value="<c:out value='${form.displayName}'/>"
                       <c:if test="${not empty errors.displayName}">aria-invalid="true" aria-describedby="displayName-error"</c:if>>
                <c:if test="${not empty errors.displayName}">
                    <p class="field-error" id="displayName-error"><c:out value="${errors.displayName}"/></p>
                </c:if>
            </div>

            <div class="field">
                <label for="role">Role <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <select id="role" name="role" required
                        <c:if test="${not empty errors.role}">aria-invalid="true" aria-describedby="role-error"</c:if>>
                    <c:forEach var="role" items="${roles}">
                        <option value="${role.code}" ${form.role == role.code ? 'selected' : ''}>
                            <c:out value="${role.name}"/>
                        </option>
                    </c:forEach>
                </select>
                <c:if test="${not empty errors.role}">
                    <p class="field-error" id="role-error"><c:out value="${errors.role}"/></p>
                </c:if>
            </div>

            <div class="field">
                <label for="password">Initial password <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="password" name="password" type="password" required autocomplete="new-password"
                       <c:if test="${not empty errors.password}">aria-invalid="true" aria-describedby="password-error"</c:if>>
                <c:if test="${not empty errors.password}">
                    <p class="field-error" id="password-error"><c:out value="${errors.password}"/></p>
                </c:if>
            </div>
        </div>

        <p class="hint">All fields are required.</p>

        <div class="form-actions">
            <button class="btn" type="submit">Create account</button>
            <a class="btn btn-ghost" href="${ctx}/app/security">Cancel</a>
        </div>
    </form>
</section>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
