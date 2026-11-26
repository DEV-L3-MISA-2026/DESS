<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<c:if test="${page.hasEntry}">
    <section class="panel" aria-labelledby="entry-title">
        <div class="panel-head">
            <div>
                <h2 class="section-title" id="entry-title">Enter results: <c:out value="${page.entry.assessmentTitle}"/></h2>
                <p class="panel-note"><c:out value="${page.entry.countLabel}"/></p>
            </div>
        </div>
        <p class="panel-note">Course: <c:out value="${page.entry.course}"/> · Type:
        <c:out value="${page.entry.type}"/> · Maximum score:
        <c:out value="${page.entry.maximumScore}"/> · Session:
        <c:out value="${page.entry.sessionStartsAt}"/> to
        <c:out value="${page.entry.sessionEndsAt}"/> (UTC) ·
        Room: <c:out value="${page.entry.room}"/></p>
        <p class="panel-note">No academic period is linked yet: course offerings do not exist
        yet, so no period can be shown here.</p>

        <c:if test="${not empty page.entry.errors.form}">
            <div class="flash error" role="alert"><c:out value="${page.entry.errors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${page.entry.errors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>

        <c:choose>
            <c:when test="${empty page.entry.rows}">
                <c:set var="emptyState" value="${page.entry.rowsEmpty}"/>
                <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
            </c:when>
            <c:otherwise>
                <div class="table-wrap" tabindex="0" role="region" aria-label="Candidates and stored results">
                    <table>
                        <thead>
                        <tr>
                            <th scope="col">Registration number</th>
                            <th scope="col">Candidate</th>
                            <th scope="col">Stored mark</th>
                            <th scope="col">Status</th>
                            <th scope="col">Corrections</th>
                            <c:if test="${canWrite}">
                                <th scope="col">Enter or correct</th>
                            </c:if>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="row" items="${page.entry.rows}">
                            <tr>
                                <td class="mono"><c:out value="${row.registrationNumber}"/></td>
                                <td><c:out value="${row.name}"/></td>
                                <td><c:out value="${row.storedScore}"/></td>
                                <td>
                                    <c:set var="badge" value="${row.status}"/>
                                    <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty row.corrections}">—</c:when>
                                        <c:otherwise>
                                            <ul>
                                                <c:forEach var="correction" items="${row.corrections}">
                                                    <li><c:out value="${correction.previousScore}"/> (<c:out value="${correction.previousStatus}"/>) → <c:out value="${correction.newScore}"/> (<c:out value="${correction.newStatus}"/>), by <c:out value="${correction.correctedBy}"/>, <c:out value="${correction.correctedAt}"/> UTC</li>
                                                </c:forEach>
                                            </ul>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <c:if test="${canWrite}">
                                    <td>
                                        <form method="post" action="${ctx}/app/results" class="row">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="action" value="${empty row.resultId ? 'recordResult' : 'correctResult'}">
                                            <input type="hidden" name="assessmentId" value="${page.entry.assessmentId}">
                                            <input type="hidden" name="sessionId" value="${page.entry.sessionId}">
                                            <input type="hidden" name="studentPersonId" value="${row.studentPersonId}">
                                            <input type="hidden" name="resultId" value="${row.resultId}">
                                            <c:set var="scoreKey" value="${fn:concat('score-', row.studentPersonId)}"/>
                                            <c:set var="statusKey" value="${fn:concat('status-', row.studentPersonId)}"/>
                                            <div class="field">
                                                <label for="score-${row.studentPersonId}">Mark</label>
                                                <input id="score-${row.studentPersonId}" name="score" type="text" inputmode="decimal"
                                                       value="<c:out value='${row.studentPersonId == page.entry.form.studentPersonId ? page.entry.form.score : ""}'/>"
                                                       <c:if test="${not empty page.entry.errors[scoreKey]}">aria-invalid="true" aria-describedby="score-${row.studentPersonId}-error"</c:if>>
                                                <c:if test="${not empty page.entry.errors[scoreKey]}">
                                                    <p class="field-error" id="score-${row.studentPersonId}-error"><c:out value="${page.entry.errors[scoreKey]}"/></p>
                                                </c:if>
                                            </div>
                                            <div class="field">
                                                <label for="status-${row.studentPersonId}">Status</label>
                                                <select id="status-${row.studentPersonId}" name="status"
                                                        <c:if test="${not empty page.entry.errors[statusKey]}">aria-invalid="true" aria-describedby="status-${row.studentPersonId}-error"</c:if>>
                                                    <option value="">Choose a status</option>
                                                    <option value="SCORED" ${row.studentPersonId == page.entry.form.studentPersonId and page.entry.form.status == 'SCORED' ? 'selected' : ''}>Scored</option>
                                                    <option value="ABSENT" ${row.studentPersonId == page.entry.form.studentPersonId and page.entry.form.status == 'ABSENT' ? 'selected' : ''}>Absent</option>
                                                    <option value="EXCUSED" ${row.studentPersonId == page.entry.form.studentPersonId and page.entry.form.status == 'EXCUSED' ? 'selected' : ''}>Excused</option>
                                                </select>
                                                <c:if test="${not empty page.entry.errors[statusKey]}">
                                                    <p class="field-error" id="status-${row.studentPersonId}-error"><c:out value="${page.entry.errors[statusKey]}"/></p>
                                                </c:if>
                                            </div>
                                            <div class="form-actions compact">
                                                <button class="btn btn-sm" type="submit">${empty row.resultId ? 'Save result' : 'Correct result'}</button>
                                            </div>
                                        </form>
                                    </td>
                                </c:if>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <c:if test="${not canWrite}">
                    <p class="panel-note">Result entry needs the academic write permission.
                    Professors see the stored results only.</p>
                </c:if>
            </c:otherwise>
        </c:choose>
    </section>
</c:if>

<section class="panel" aria-labelledby="sessions-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="sessions-title">Exam sessions</h2>
            <p class="panel-note"><c:out value="${page.countLabel}"/></p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty page.sessions}">
            <c:set var="emptyState" value="${page.sessionsEmpty}"/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Exam sessions and results">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Starts (UTC)</th>
                        <th scope="col">Ends (UTC)</th>
                        <th scope="col">Room</th>
                        <th scope="col">Candidates</th>
                        <th scope="col">Results</th>
                        <th scope="col">Entry</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="session" items="${page.sessions}">
                        <tr>
                            <td class="mono"><c:out value="${session.startsAt}"/></td>
                            <td class="mono"><c:out value="${session.endsAt}"/></td>
                            <td><c:out value="${session.room}"/></td>
                            <td><c:out value="${session.candidateCount}"/></td>
                            <td>
                                <c:set var="badge" value="${session.result}"/>
                                <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                            </td>
                            <td>
                                <a href="${ctx}${session.entryHref}">Enter results</a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
