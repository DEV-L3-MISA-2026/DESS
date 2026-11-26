<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="assessment-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="assessment-title"><c:out value="${page.assessmentTitle}"/></h2>
            <p class="panel-note"><c:out value="${page.countLabel}"/></p>
        </div>
        <c:set var="badge" value="${page.status}"/>
        <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
    </div>

    <p class="panel-note">Course: <c:out value="${page.course}"/> · Type:
    <c:out value="${page.type}"/> · Maximum score:
    <c:out value="${page.maximumScore}"/> · Duration:
    <c:out value="${page.durationMinutes}"/> minutes</p>

    <c:choose>
        <c:when test="${empty page.eligibleCandidates and empty page.ineligibleCandidates}">
            <c:set var="emptyState" value="${page.candidatesEmpty}"/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <h3 class="form-section-title">Eligible candidates</h3>
            <c:choose>
                <c:when test="${empty page.eligibleCandidates}">
                    <c:set var="emptyState" value="${page.eligibleEmpty}"/>
                    <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap" tabindex="0" role="region" aria-label="Eligible candidates">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col">Registration number</th>
                                <th scope="col">Candidate</th>
                                <th scope="col">Person id</th>
                                <th scope="col">Eligibility</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="candidate" items="${page.eligibleCandidates}">
                                <tr>
                                    <td class="mono"><c:out value="${candidate.registrationNumber}"/></td>
                                    <td><c:out value="${candidate.name}"/></td>
                                    <td><c:out value="${candidate.personId}"/></td>
                                    <td>
                                        <c:set var="badge" value="${candidate.eligibility}"/>
                                        <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>

            <h3 class="form-section-title">Ineligible candidates</h3>
            <c:choose>
                <c:when test="${empty page.ineligibleCandidates}">
                    <c:set var="emptyState" value="${page.ineligibleEmpty}"/>
                    <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap" tabindex="0" role="region" aria-label="Ineligible candidates">
                        <table>
                            <thead>
                            <tr>
                                <th scope="col">Registration number</th>
                                <th scope="col">Candidate</th>
                                <th scope="col">Person id</th>
                                <th scope="col">Eligibility</th>
                                <th scope="col">Reason</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="candidate" items="${page.ineligibleCandidates}">
                                <tr>
                                    <td class="mono"><c:out value="${candidate.registrationNumber}"/></td>
                                    <td><c:out value="${candidate.name}"/></td>
                                    <td><c:out value="${candidate.personId}"/></td>
                                    <td>
                                        <c:set var="badge" value="${candidate.eligibility}"/>
                                        <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                                    </td>
                                    <td><c:out value="${candidate.reasonMessage}"/></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
</section>

<section class="panel" aria-labelledby="check-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="check-title">Check other students</h2>
            <p class="panel-note">Candidates come from the exam sessions of this assessment.
            When no session exists yet, enter student person ids to check them
            explicitly. The list stays in the address, so reloading the page
            repeats the same check.</p>
        </div>
    </div>
    <c:if test="${not empty page.errors.form}">
        <div class="flash error" role="alert"><c:out value="${page.errors.form}"/></div>
    </c:if>
    <c:set var="validationErrors" value="${page.errors}"/>
    <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
    <form method="get" action="${ctx}/app/eligibility" class="row">
        <input type="hidden" name="assessment" value="${param.assessment}">
        <div class="field grow">
            <label for="students">Student person ids</label>
            <input id="students" name="students" type="text"
                   value="<c:out value='${page.students}'/>"
                   placeholder="For example: 12, 34"
                   <c:if test="${not empty page.errors.students}">aria-invalid="true" aria-describedby="students-error"</c:if>>
            <p class="hint">Comma-separated person ids. Checked in addition to the session
            candidates.</p>
            <c:if test="${not empty page.errors.students}">
                <p class="field-error" id="students-error"><c:out value="${page.errors.students}"/></p>
            </c:if>
        </div>
        <div class="form-actions compact">
            <button class="btn" type="submit">Check students</button>
        </div>
    </form>
</section>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
