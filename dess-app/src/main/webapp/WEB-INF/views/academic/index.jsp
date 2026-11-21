<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Academic"/>
<c:set var="topbarContext" value="Catalogue and calendar"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="pageDescription" value="Periods set the calendar; programs describe credentials; courses form the catalogue. A course is not yet an offering."/>
<c:set var="breadcrumbParentLabel" value="Overview"/>
<c:set var="breadcrumbParentHref" value="${ctx}/app"/>
<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="periods-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="periods-title">Academic periods</h2>
            <p class="panel-note"><c:out value="${fn:length(periods)}"/>
                <c:out value="${fn:length(periods) == 1 ? 'period' : 'periods'}"/>.</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty periods}">
            <c:set var="emptyTitle" value="No academic period yet."/>
            <c:set var="emptyMessage" value="A period is the year and term that academic operations refer to."/>
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
                    <c:forEach var="period" items="${periods}">
                        <tr>
                            <td class="mono"><c:out value="${period.academicYear}"/></td>
                            <td>Term <c:out value="${period.term}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${canWrite}">
        <h3 class="form-section-title">Add a period</h3>
        <c:if test="${not empty periodErrors.form}">
            <div class="flash error" role="alert"><c:out value="${periodErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${periodErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/academic" class="form-row">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createPeriod">
            <div class="field">
                <label for="academicYear">Academic year <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="academicYear" name="academicYear" type="text" required
                       placeholder="2026-2027"
                       value="<c:out value='${periodForm.academicYear}'/>"
                       <c:if test="${not empty periodErrors.academicYear}">aria-invalid="true" aria-describedby="academicYear-error"</c:if>>
                <c:if test="${not empty periodErrors.academicYear}">
                    <p class="field-error" id="academicYear-error"><c:out value="${periodErrors.academicYear}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="term">Term <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <select id="term" name="term" required
                        <c:if test="${not empty periodErrors.term}">aria-invalid="true" aria-describedby="term-error"</c:if>>
                    <option value="1" ${periodForm.term == '1' ? 'selected' : ''}>1</option>
                    <option value="2" ${periodForm.term == '2' ? 'selected' : ''}>2</option>
                    <option value="3" ${periodForm.term == '3' ? 'selected' : ''}>3</option>
                </select>
                <c:if test="${not empty periodErrors.term}">
                    <p class="field-error" id="term-error"><c:out value="${periodErrors.term}"/></p>
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
            <p class="panel-note"><c:out value="${fn:length(programs)}"/>
                <c:out value="${fn:length(programs) == 1 ? 'program' : 'programs'}"/>.</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty programs}">
            <c:set var="emptyTitle" value="No program yet."/>
            <c:set var="emptyMessage" value="Create a program before adding its versions and enrollments."/>
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
                    <c:forEach var="program" items="${programs}">
                        <tr>
                            <td class="mono"><c:out value="${program.code}"/></td>
                            <td><c:out value="${program.name}"/></td>
                            <td>
                                <c:set var="badgeTone" value="${program.status == 'ACTIVE' ? 'ok' : 'off'}"/>
                                <c:set var="badgeLabel" value="${program.status == 'ACTIVE' ? 'Active' : 'Inactive'}"/>
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
        <c:if test="${not empty programErrors.form}">
            <div class="flash error" role="alert"><c:out value="${programErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${programErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/academic" class="form-row">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createProgram">
            <div class="field">
                <label for="programCode">Code <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="programCode" name="code" type="text" required
                       value="<c:out value='${programForm.code}'/>"
                       <c:if test="${not empty programErrors.programCode}">aria-invalid="true" aria-describedby="programCode-error"</c:if>>
                <c:if test="${not empty programErrors.programCode}">
                    <p class="field-error" id="programCode-error"><c:out value="${programErrors.programCode}"/></p>
                </c:if>
            </div>
            <div class="field grow">
                <label for="programName">Name <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="programName" name="name" type="text" required
                       value="<c:out value='${programForm.name}'/>"
                       <c:if test="${not empty programErrors.programName}">aria-invalid="true" aria-describedby="programName-error"</c:if>>
                <c:if test="${not empty programErrors.programName}">
                    <p class="field-error" id="programName-error"><c:out value="${programErrors.programName}"/></p>
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
            <p class="panel-note"><c:out value="${fn:length(courses)}"/>
                <c:out value="${fn:length(courses) == 1 ? 'course' : 'courses'}"/> in the catalogue.</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty courses}">
            <c:set var="emptyTitle" value="No course yet."/>
            <c:set var="emptyMessage" value="Add the first course to the catalogue."/>
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
                    <c:forEach var="course" items="${courses}">
                        <tr>
                            <td class="mono"><c:out value="${course.code}"/></td>
                            <td><c:out value="${course.title}"/></td>
                            <td>
                                <c:set var="badgeTone" value="${course.active ? 'ok' : 'off'}"/>
                                <c:set var="badgeLabel" value="${course.active ? 'Active' : 'Inactive'}"/>
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
        <c:if test="${not empty courseErrors.form}">
            <div class="flash error" role="alert"><c:out value="${courseErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${courseErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/academic" class="form-row">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createCourse">
            <div class="field">
                <label for="courseCode">Code <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="courseCode" name="code" type="text" required
                       value="<c:out value='${courseForm.code}'/>"
                       <c:if test="${not empty courseErrors.courseCode}">aria-invalid="true" aria-describedby="courseCode-error"</c:if>>
                <c:if test="${not empty courseErrors.courseCode}">
                    <p class="field-error" id="courseCode-error"><c:out value="${courseErrors.courseCode}"/></p>
                </c:if>
            </div>
            <div class="field grow">
                <label for="courseTitle">Title <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="courseTitle" name="title" type="text" required
                       value="<c:out value='${courseForm.title}'/>"
                       <c:if test="${not empty courseErrors.courseTitle}">aria-invalid="true" aria-describedby="courseTitle-error"</c:if>>
                <c:if test="${not empty courseErrors.courseTitle}">
                    <p class="field-error" id="courseTitle-error"><c:out value="${courseErrors.courseTitle}"/></p>
                </c:if>
            </div>
            <div class="form-actions compact">
                <button class="btn" type="submit">Add course</button>
            </div>
        </form>
    </c:if>
</section>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
