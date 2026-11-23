<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="periods-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="periods-title">Academic periods</h2>
            <p class="panel-note"><c:out value="${page.periodCountLabel}"/></p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty page.periods}">
            <c:set var="emptyState" value="${page.periodsEmpty}"/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Academic periods">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Academic year</th>
                        <th scope="col">Term</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="period" items="${page.periods}">
                        <tr>
                            <td class="mono"><c:out value="${period.academicYear}"/></td>
                            <td><c:out value="${period.termLabel}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${canWrite}">
        <h3 class="form-section-title">Add a period</h3>
        <c:if test="${not empty page.periodErrors.form}">
            <div class="flash error" role="alert"><c:out value="${page.periodErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${page.periodErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/academic" class="form-row">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createPeriod">
            <div class="field">
                <label for="academicYear">Academic year <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="academicYear" name="academicYear" type="text" required
                       placeholder="2026-2027"
                       value="<c:out value='${page.periodForm.academicYear}'/>"
                       <c:if test="${not empty page.periodErrors.academicYear}">aria-invalid="true" aria-describedby="academicYear-error"</c:if>>
                <c:if test="${not empty page.periodErrors.academicYear}">
                    <p class="field-error" id="academicYear-error"><c:out value="${page.periodErrors.academicYear}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="term">Term <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <select id="term" name="term" required
                        <c:if test="${not empty page.periodErrors.term}">aria-invalid="true" aria-describedby="term-error"</c:if>>
                    <c:forEach var="option" items="${page.termOptions}">
                        <option value="${option.value}" ${option.selectedAttribute}><c:out value="${option.label}"/></option>
                    </c:forEach>
                </select>
                <c:if test="${not empty page.periodErrors.term}">
                    <p class="field-error" id="term-error"><c:out value="${page.periodErrors.term}"/></p>
                </c:if>
            </div>
            <div class="form-actions compact">
                <button class="btn" type="submit">Add period</button>
            </div>
        </form>
    </c:if>
</section>

<section class="panel" aria-labelledby="programs-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="programs-title">Programs</h2>
            <p class="panel-note"><c:out value="${page.programCountLabel}"/></p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty page.programs}">
            <c:set var="emptyState" value="${page.programsEmpty}"/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Academic programs">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Code</th>
                        <th scope="col">Name</th>
                        <th scope="col">Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="program" items="${page.programs}">
                        <tr>
                            <td class="mono"><c:out value="${program.code}"/></td>
                            <td><c:out value="${program.name}"/></td>
                            <td>
                                <c:set var="badge" value="${program.status}"/>
                                <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${canWrite}">
        <h3 class="form-section-title">Add a program</h3>
        <c:if test="${not empty page.programErrors.form}">
            <div class="flash error" role="alert"><c:out value="${page.programErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${page.programErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/academic" class="form-row">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createProgram">
            <div class="field">
                <label for="programCode">Code <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="programCode" name="code" type="text" required
                       value="<c:out value='${page.programForm.code}'/>"
                       <c:if test="${not empty page.programErrors.programCode}">aria-invalid="true" aria-describedby="programCode-error"</c:if>>
                <c:if test="${not empty page.programErrors.programCode}">
                    <p class="field-error" id="programCode-error"><c:out value="${page.programErrors.programCode}"/></p>
                </c:if>
            </div>
            <div class="field grow">
                <label for="programName">Name <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="programName" name="name" type="text" required
                       value="<c:out value='${page.programForm.name}'/>"
                       <c:if test="${not empty page.programErrors.programName}">aria-invalid="true" aria-describedby="programName-error"</c:if>>
                <c:if test="${not empty page.programErrors.programName}">
                    <p class="field-error" id="programName-error"><c:out value="${page.programErrors.programName}"/></p>
                </c:if>
            </div>
            <div class="form-actions compact">
                <button class="btn" type="submit">Add program</button>
            </div>
        </form>
    </c:if>
</section>

<section class="panel" aria-labelledby="courses-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="courses-title">Courses</h2>
            <p class="panel-note"><c:out value="${page.courseCountLabel}"/></p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty page.courses}">
            <c:set var="emptyState" value="${page.coursesEmpty}"/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Course catalogue">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Code</th>
                        <th scope="col">Title</th>
                        <th scope="col">Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="course" items="${page.courses}">
                        <tr>
                            <td class="mono"><c:out value="${course.code}"/></td>
                            <td><c:out value="${course.title}"/></td>
                            <td>
                                <c:set var="badge" value="${course.status}"/>
                                <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${canWrite}">
        <h3 class="form-section-title">Add a course</h3>
        <c:if test="${not empty page.courseErrors.form}">
            <div class="flash error" role="alert"><c:out value="${page.courseErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${page.courseErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/academic" class="form-row">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createCourse">
            <div class="field">
                <label for="courseCode">Code <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="courseCode" name="code" type="text" required
                       value="<c:out value='${page.courseForm.code}'/>"
                       <c:if test="${not empty page.courseErrors.courseCode}">aria-invalid="true" aria-describedby="courseCode-error"</c:if>>
                <c:if test="${not empty page.courseErrors.courseCode}">
                    <p class="field-error" id="courseCode-error"><c:out value="${page.courseErrors.courseCode}"/></p>
                </c:if>
            </div>
            <div class="field grow">
                <label for="courseTitle">Title <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="courseTitle" name="title" type="text" required
                       value="<c:out value='${page.courseForm.title}'/>"
                       <c:if test="${not empty page.courseErrors.courseTitle}">aria-invalid="true" aria-describedby="courseTitle-error"</c:if>>
                <c:if test="${not empty page.courseErrors.courseTitle}">
                    <p class="field-error" id="courseTitle-error"><c:out value="${page.courseErrors.courseTitle}"/></p>
                </c:if>
            </div>
            <div class="form-actions compact">
                <button class="btn" type="submit">Add course</button>
            </div>
        </form>
    </c:if>
</section>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
