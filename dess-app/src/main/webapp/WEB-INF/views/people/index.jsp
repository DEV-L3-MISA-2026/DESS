<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="People"/>
<c:set var="topbarContext" value="Directory"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<div class="dess-page-header">
    <div class="dess-page-heading">
        <nav class="dess-breadcrumb" aria-label="Breadcrumb">
            <ol>
                <li><a href="${ctx}/app">Overview</a></li>
                <li aria-current="page">People</li>
            </ol>
        </nav>
        <h1 class="dess-page-title">People</h1>
        <p class="dess-page-description">
            Students and their registration numbers. A person record is not a sign-in account.
        </p>
    </div>
    <c:if test="${canWrite}">
        <div class="header-actions">
            <a class="btn" href="#create-student">Add student</a>
        </div>
    </c:if>
</div>

<section class="panel" aria-labelledby="student-list-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="student-list-title">Students</h2>
            <p class="panel-note"><c:out value="${fn:length(students)}"/> record(s) shown.</p>
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
            <div class="empty">
                <strong>No student matches this view.</strong>
                <p>
                    <c:choose>
                        <c:when test="${not empty q}">Try a shorter search or clear the filter.</c:when>
                        <c:otherwise>Create the first student to start the directory.</c:otherwise>
                    </c:choose>
                </p>
                <c:if test="${canWrite and empty q}">
                    <a class="btn" href="#create-student">Add student</a>
                </c:if>
            </div>
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
                                <span class="badge ${student.active ? 'ok' : 'off'}">
                                    <c:out value="${student.active ? 'Active' : 'Inactive'}"/>
                                </span>
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

        <form method="post" action="${ctx}/app/people" class="stack">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="create">

            <div class="field-grid">
                <div class="field">
                    <label for="firstName">First name <span class="req" aria-hidden="true">*</span></label>
                    <input id="firstName" name="firstName" type="text" required
                           value="<c:out value='${form.firstName}'/>"
                           <c:if test="${not empty errors.firstName}">aria-invalid="true" aria-describedby="firstName-error"</c:if>>
                    <c:if test="${not empty errors.firstName}">
                        <p class="field-error" id="firstName-error"><c:out value="${errors.firstName}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="lastName">Last name <span class="req" aria-hidden="true">*</span></label>
                    <input id="lastName" name="lastName" type="text" required
                           value="<c:out value='${form.lastName}'/>"
                           <c:if test="${not empty errors.lastName}">aria-invalid="true" aria-describedby="lastName-error"</c:if>>
                    <c:if test="${not empty errors.lastName}">
                        <p class="field-error" id="lastName-error"><c:out value="${errors.lastName}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="email">Email address <span class="req" aria-hidden="true">*</span></label>
                    <input id="email" name="email" type="email" required
                           value="<c:out value='${form.email}'/>"
                           <c:if test="${not empty errors.email}">aria-invalid="true" aria-describedby="email-error"</c:if>>
                    <c:if test="${not empty errors.email}">
                        <p class="field-error" id="email-error"><c:out value="${errors.email}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="registrationNumber">Registration number <span class="req" aria-hidden="true">*</span></label>
                    <input id="registrationNumber" name="registrationNumber" type="text" required
                           value="<c:out value='${form.registrationNumber}'/>"
                           <c:if test="${not empty errors.registrationNumber}">aria-invalid="true" aria-describedby="registrationNumber-error"</c:if>>
                    <c:if test="${not empty errors.registrationNumber}">
                        <p class="field-error" id="registrationNumber-error"><c:out value="${errors.registrationNumber}"/></p>
                    </c:if>
                </div>
            </div>

            <p class="hint">Fields marked <span class="req" aria-hidden="true">*</span> are required.</p>

            <div class="form-actions">
                <button class="btn" type="submit">Create student</button>
                <a class="btn btn-ghost" href="${ctx}/app/people">Cancel</a>
            </div>
        </form>
    </section>
</c:if>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
