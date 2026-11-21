<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Results"/>
<c:set var="topbarContext" value="Results by candidate"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<div class="dess-page-header">
    <div class="dess-page-heading">
        <nav class="dess-breadcrumb" aria-label="Breadcrumb">
            <ol>
                <li><a href="${ctx}/app">Overview</a></li>
                <li aria-current="page">Results</li>
            </ol>
        </nav>
        <h1 class="dess-page-title">Results</h1>
        <p class="dess-page-description">
            Results for each candidate of an exam session. Result entry is the next step
            of the assessment module, after the assessment definitions and the eligibility
            of candidates.
        </p>
    </div>
</div>

<section class="panel" aria-labelledby="about-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="about-title">About results</h2>
        </div>
    </div>
    <p class="panel-note">The Result model does not exist yet. No mark can be entered or shown
    here until the results model and the result entry work are done. Until then, every
    session below shows its results as not entered yet.</p>
</section>

<section class="panel" aria-labelledby="sessions-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="sessions-title">Exam sessions</h2>
            <p class="panel-note"><c:out value="${fn:length(sessions)}"/> session(s).</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty sessions}">
            <div class="empty">
                <strong>No exam session yet.</strong>
                <p>Results are entered per candidate once a session exists and the results
                model is ready.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Exam sessions">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Starts</th>
                        <th scope="col">Ends</th>
                        <th scope="col">Room</th>
                        <th scope="col">Candidates</th>
                        <th scope="col">Results</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="session" items="${sessions}">
                        <tr>
                            <td class="mono"><c:out value="${session.startsAt}"/></td>
                            <td class="mono"><c:out value="${session.endsAt}"/></td>
                            <td><c:out value="${session.room}"/></td>
                            <td><c:out value="${session.candidateCount}"/></td>
                            <td>
                                <span class="badge off">
                                    <c:out value="${session.results}"/>
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

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
