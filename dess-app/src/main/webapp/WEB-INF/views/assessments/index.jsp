<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="assessments-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="assessments-title">Assessments</h2>
            <p class="panel-note"><c:out value="${page.countLabel}"/></p>
        </div>
    </div>

    <form method="get" action="${ctx}/app/assessments" class="row" role="search">
        <div class="field grow">
            <label for="filterType">Type</label>
            <select id="filterType" name="type">
                <c:forEach var="option" items="${page.typeOptions}">
                    <option value="${option.value}" ${option.selectedAttribute}><c:out value="${option.label}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="field grow">
            <label for="filterCourse">Course</label>
            <select id="filterCourse" name="course">
                <c:forEach var="option" items="${page.courseOptions}">
                    <option value="${option.value}" ${option.selectedAttribute}><c:out value="${option.label}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="form-actions compact">
            <button class="btn" type="submit">Filter</button>
            <c:if test="${not empty page.typeFilter or not empty page.courseFilter}">
                <a class="btn btn-ghost" href="${ctx}/app/assessments">Clear</a>
            </c:if>
        </div>
    </form>
    <p class="hint">The filters stay in the address: reloading the page restores them.
    There is no period filter yet, because an assessment has no period until
    course offerings exist.</p>

    <c:choose>
        <c:when test="${empty page.assessments}">
            <c:set var="emptyState" value="${page.assessmentsEmpty}"/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Assessments">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Title</th>
                        <th scope="col">Type</th>
                        <th scope="col">Course</th>
                        <th scope="col">Maximum score</th>
                        <th scope="col">Duration (min)</th>
                        <th scope="col">Status</th>
                        <th scope="col">Candidates</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="assessment" items="${page.assessments}">
                        <tr>
                            <td><c:out value="${assessment.title}"/></td>
                            <td><c:out value="${assessment.type}"/></td>
                            <td><c:out value="${assessment.course}"/></td>
                            <td><c:out value="${assessment.maximumScore}"/></td>
                            <td><c:out value="${assessment.durationMinutes}"/></td>
                            <td>
                                <c:set var="badge" value="${assessment.status}"/>
                                <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                            </td>
                            <td>
                                <a href="${ctx}${assessment.eligibilityHref}">View candidates</a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${canWrite}">
        <h3 class="form-section-title">Create an assessment</h3>
        <c:if test="${not empty page.errors.form}">
            <div class="flash error" role="alert"><c:out value="${page.errors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${page.errors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/assessments" class="grid">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createAssessment">
            <div class="field">
                <label for="title">Title <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="title" name="title" type="text" required maxlength="200"
                       value="<c:out value='${page.form.title}'/>"
                       <c:if test="${not empty page.errors.title}">aria-invalid="true" aria-describedby="title-error"</c:if>>
                <c:if test="${not empty page.errors.title}">
                    <p class="field-error" id="title-error"><c:out value="${page.errors.title}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="type">Type <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <select id="type" name="type" required
                        <c:if test="${not empty page.errors.type}">aria-invalid="true" aria-describedby="type-error"</c:if>>
                    <option value="">Choose a type</option>
                    <c:forEach var="option" items="${page.createTypes}">
                        <option value="${option.value}" ${option.selectedAttribute}><c:out value="${option.label}"/></option>
                    </c:forEach>
                </select>
                <c:if test="${not empty page.errors.type}">
                    <p class="field-error" id="type-error"><c:out value="${page.errors.type}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="courseId">Course <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <select id="courseId" name="courseId" required
                        <c:if test="${not empty page.errors.courseId}">aria-invalid="true" aria-describedby="courseId-error"</c:if>>
                    <option value="">Choose a course</option>
                    <c:forEach var="option" items="${page.createCourses}">
                        <option value="${option.value}" ${option.selectedAttribute}><c:out value="${option.label}"/></option>
                    </c:forEach>
                </select>
                <c:if test="${not empty page.errors.courseId}">
                    <p class="field-error" id="courseId-error"><c:out value="${page.errors.courseId}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="maximumScore">Maximum score <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="maximumScore" name="maximumScore" type="text" required inputmode="decimal"
                       value="<c:out value='${page.form.maximumScore}'/>"
                       <c:if test="${not empty page.errors.maximumScore}">aria-invalid="true" aria-describedby="maximumScore-error"</c:if>>
                <p class="hint">Greater than zero, at most two decimal places, at most 9999.99.</p>
                <c:if test="${not empty page.errors.maximumScore}">
                    <p class="field-error" id="maximumScore-error"><c:out value="${page.errors.maximumScore}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="durationMinutes">Duration in minutes <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="durationMinutes" name="durationMinutes" type="number" required min="1" step="1"
                       value="<c:out value='${page.form.durationMinutes}'/>"
                       <c:if test="${not empty page.errors.durationMinutes}">aria-invalid="true" aria-describedby="durationMinutes-error"</c:if>>
                <p class="hint">A whole number of minutes, greater than zero.</p>
                <c:if test="${not empty page.errors.durationMinutes}">
                    <p class="field-error" id="durationMinutes-error"><c:out value="${page.errors.durationMinutes}"/></p>
                </c:if>
            </div>
            <div class="form-actions span-2">
                <button class="btn" type="submit">Create assessment</button>
            </div>
        </form>
    </c:if>
</section>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
