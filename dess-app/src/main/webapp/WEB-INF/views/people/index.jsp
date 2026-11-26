<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="student-list-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="student-list-title">Students</h2>
            <p class="panel-note"><c:out value="${page.countLabel}"/></p>
        </div>
        <form class="toolbar" method="get" action="${ctx}/app/people" role="search">
            <label class="visually-hidden" for="people-q">Search people</label>
            <input id="people-q" name="q" type="search" value="<c:out value='${page.query}'/>"
                   placeholder="Name, email or registration number">
            <button class="btn btn-sm" type="submit">Search</button>
            <c:if test="${not empty page.query}">
                <a class="btn btn-ghost btn-sm" href="${ctx}/app/people">Clear</a>
            </c:if>
            <c:if test="${param.includeInactive == 'true'}">
                <input type="hidden" name="includeInactive" value="true">
            </c:if>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty page.students}">
            <c:set var="emptyState" value="${page.studentsEmpty}"/>
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
                        <th scope="col">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="student" items="${page.students}">
                        <tr>
                            <td><c:out value="${student.name}"/></td>
                            <td><c:out value="${student.email}"/></td>
                            <td class="mono"><c:out value="${student.registrationNumber}"/></td>
                            <td>
                                <c:set var="badge" value="${student.status}"/>
                                <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                            </td>
                            <td>
                                <c:if test="${canWrite}">
                                    <details>
                                        <summary class="btn btn-ghost btn-sm">Edit</summary>
                                        <form method="post" action="${ctx}/app/people" class="stack">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="action" value="update">
                                            <input type="hidden" name="id" value="${student.id}">
                                            <label>First name
                                                <input name="firstName" value="<c:out value='${student.firstName}'/>" required>
                                            </label>
                                            <label>Last name
                                                <input name="lastName" value="<c:out value='${student.lastName}'/>" required>
                                            </label>
                                            <label>Email
                                                <input name="email" type="email" value="<c:out value='${student.email}'/>" required>
                                            </label>
                                            <button class="btn btn-sm" type="submit">Save</button>
                                        </form>
                                    </details>
                                    <c:if test="${student.status.label == 'Active'}">
                                        <form method="post" action="${ctx}/app/people" class="inline">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="action" value="deactivate">
                                            <input type="hidden" name="id" value="${student.id}">
                                            <button class="btn btn-danger btn-sm" type="submit">Deactivate</button>
                                        </form>
                                    </c:if>
                                </c:if>
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

        <c:if test="${not empty page.errors.form}">
            <div class="flash error" role="alert"><c:out value="${page.errors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${page.errors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>

        <form method="post" action="${ctx}/app/people" class="stack">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="create">

            <div class="field-grid">
                <div class="field">
                    <label for="firstName">First name <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                    <input id="firstName" name="firstName" type="text" required
                           value="<c:out value='${page.form.firstName}'/>"
                           <c:if test="${not empty page.errors.firstName}">aria-invalid="true" aria-describedby="firstName-error"</c:if>>
                    <c:if test="${not empty page.errors.firstName}">
                        <p class="field-error" id="firstName-error"><c:out value="${page.errors.firstName}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="lastName">Last name <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                    <input id="lastName" name="lastName" type="text" required
                           value="<c:out value='${page.form.lastName}'/>"
                           <c:if test="${not empty page.errors.lastName}">aria-invalid="true" aria-describedby="lastName-error"</c:if>>
                    <c:if test="${not empty page.errors.lastName}">
                        <p class="field-error" id="lastName-error"><c:out value="${page.errors.lastName}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="email">Email address <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                    <input id="email" name="email" type="email" required
                           value="<c:out value='${page.form.email}'/>"
                           <c:if test="${not empty page.errors.email}">aria-invalid="true" aria-describedby="email-error"</c:if>>
                    <c:if test="${not empty page.errors.email}">
                        <p class="field-error" id="email-error"><c:out value="${page.errors.email}"/></p>
                    </c:if>
                </div>

                <div class="field">
                    <label for="registrationNumber">Registration number <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                    <input id="registrationNumber" name="registrationNumber" type="text" required
                           value="<c:out value='${page.form.registrationNumber}'/>"
                           <c:if test="${not empty page.errors.registrationNumber}">aria-invalid="true" aria-describedby="registrationNumber-error"</c:if>>
                    <c:if test="${not empty page.errors.registrationNumber}">
                        <p class="field-error" id="registrationNumber-error"><c:out value="${page.errors.registrationNumber}"/></p>
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
