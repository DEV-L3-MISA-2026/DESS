<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="People"/>
<c:set var="topbarContext" value="Directory"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="pageDescription" value="Students and their registration numbers. A person record is not a sign-in account."/>
<c:set var="breadcrumbParentLabel" value="Overview"/>
<c:set var="breadcrumbParentHref" value="${ctx}/app"/>
<c:if test="${canWrite}">
    <c:set var="headerActionLabel" value="Add student"/>
    <c:set var="headerActionHref" value="#create-student"/>
</c:if>
<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="student-list-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="student-list-title">Students</h2>
            <p class="panel-note"><c:out value="${fn:length(students)}"/>
                <c:out value="${fn:length(students) == 1 ? 'record' : 'records'}"/> shown.</p>
        </div>
        <form class="toolbar" method="get" action="${ctx}/app/people" role="search">
            <label class="visually-hidden" for="people-q">Search people</label>
            <input id="people-q" name="q" type="search" value="<c:out value='${q}'/>"
                   placeholder="Name, email or registration number">
            <button class="btn btn-sm" type="submit">Search</button>
            <c:if test="${not empty q}">
                <a class="btn btn-ghost btn-sm" href="${ctx}/app/people">Clear</a>
            </c:if>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty students}">
            <c:set var="emptyTitle" value="No student matches this view."/>
            <c:choose>
                <c:when test="${not empty q}">
                    <c:set var="emptyMessage" value="Try a shorter search or clear the filter."/>
                </c:when>
                <c:otherwise>
                    <c:set var="emptyMessage" value="Create the first student to start the directory."/>
                </c:otherwise>
            </c:choose>
            <c:if test="${canWrite and empty q}">
                <c:set var="emptyActionLabel" value="Add student"/>
                <c:set var="emptyActionHref" value="#create-student"/>
            </c:if>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Student list">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Name</th>
                        <th scope="col">Email</th>
                        <th scope="col">Registration number</th>
                        <th scope="col">Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="student" items="${students}">
                        <tr>
                            <td><c:out value="${student.name}"/></td>
                            <td><c:out value="${student.email}"/></td>
                            <td class="mono"><c:out value="${student.registrationNumber}"/></td>
                            <td>
                                <c:set var="badgeTone" value="${student.active ? 'ok' : 'off'}"/>
                                <c:set var="badgeLabel" value="${student.active ? 'Active' : 'Inactive'}"/>
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

<c:if test="${canWrite}">
    <section class="panel" id="create-student" aria-labelledby="create-student-title">
        <div class="panel-head">
            <div>
                <h2 class="section-title" id="create-student-title">Add a student</h2>
                <p class="panel-note">The email address and registration number must be unique.</p>
            </div>
        </div>

        <c:if test="${not empty errors.form}">
            <div class="flash error" role="alert"><c:out value="${errors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${errors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>

        <form method="post" action="${ctx}/app/people" class="stack">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="create">

            <div class="field-grid">
                <div class="field">
                    <label for="firstName">First name <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                    <input id="firstName" name="firstName" type="text" required
                           value="<c:out value='${form.firstName}'/>"
                           <c:if test="${not empty errors.firstName}">aria-invalid="true" aria-describedby="firstName-error"</c:if>>
                    <c:if test="${not empty errors.firstName}">
                        <p class="field-error" id="firstName-error"><c:out value="${errors.firstName}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="lastName">Last name <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                    <input id="lastName" name="lastName" type="text" required
                           value="<c:out value='${form.lastName}'/>"
                           <c:if test="${not empty errors.lastName}">aria-invalid="true" aria-describedby="lastName-error"</c:if>>
                    <c:if test="${not empty errors.lastName}">
                        <p class="field-error" id="lastName-error"><c:out value="${errors.lastName}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="email">Email address <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                    <input id="email" name="email" type="email" required
                           value="<c:out value='${form.email}'/>"
                           <c:if test="${not empty errors.email}">aria-invalid="true" aria-describedby="email-error"</c:if>>
                    <c:if test="${not empty errors.email}">
                        <p class="field-error" id="email-error"><c:out value="${errors.email}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="registrationNumber">Registration number <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                    <input id="registrationNumber" name="registrationNumber" type="text" required
                           value="<c:out value='${form.registrationNumber}'/>"
                           <c:if test="${not empty errors.registrationNumber}">aria-invalid="true" aria-describedby="registrationNumber-error"</c:if>>
                    <c:if test="${not empty errors.registrationNumber}">
                        <p class="field-error" id="registrationNumber-error"><c:out value="${errors.registrationNumber}"/></p>
                    </c:if>
                </div>
            </div>

            <p class="hint">All fields are required.</p>

            <div class="form-actions">
                <button class="btn" type="submit">Create student</button>
                <a class="btn btn-ghost" href="${ctx}/app/people">Cancel</a>
            </div>
        </form>
    </section>
</c:if>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
