<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Examinations"/>
<c:set var="topbarContext" value="Exam sessions"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<div class="dess-page-header">
    <div class="dess-page-heading">
        <nav class="dess-breadcrumb" aria-label="Breadcrumb">
            <ol>
                <li><a href="${ctx}/app">Overview</a></li>
                <li aria-current="page">Examinations</li>
            </ol>
        </nav>
        <h1 class="dess-page-title">Examinations</h1>
        <p class="dess-page-description">
            Exam sessions: one exam, in one room, at one time, with its candidates and
            invigilators. Sessions are created by the exam scheduling flow; the booking
            contract that reserves their room, group and invigilators is frozen.
        </p>
    </div>
</div>

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
                <p>Exam sessions will appear here once they are scheduled. Candidates are
                linked once eligibility is known, and invigilators are assigned afterwards.</p>
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
                        <th scope="col">Invigilators</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="session" items="${sessions}">
                        <tr>
                            <td class="mono"><c:out value="${session.startsAt}"/></td>
                            <td class="mono"><c:out value="${session.endsAt}"/></td>
                            <td><c:out value="${session.room}"/></td>
                            <td><c:out value="${session.candidateCount}"/></td>
                            <td><c:out value="${session.invigilatorCount}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
