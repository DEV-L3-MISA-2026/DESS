<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Examinations"/>
<c:set var="topbarContext" value="Exam sessions"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="pageDescription" value="Exam sessions: one assessment, in one room, at one time, with its candidates and invigilators. Sessions are created on the management page."/>
<c:set var="breadcrumbParentLabel" value="Overview"/>
<c:set var="breadcrumbParentHref" value="${ctx}/app"/>
<c:set var="headerActionLabel" value="Manage sessions"/>
<c:set var="headerActionHref" value="${ctx}/app/exam-sessions"/>
<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="sessions-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="sessions-title">Exam sessions</h2>
            <p class="panel-note"><c:out value="${fn:length(sessions)}"/> session(s). Times are in UTC.</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty sessions}">
            <c:set var="emptyTitle" value="No exam session yet."/>
            <c:set var="emptyMessage" value="Exam sessions appear here once they are scheduled. Candidates are linked once eligibility is known, and invigilators are assigned afterwards."/>
            <c:set var="emptyActionLabel" value="Manage sessions"/>
            <c:set var="emptyActionHref" value="${ctx}/app/exam-sessions"/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Exam sessions">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Starts (UTC)</th>
                        <th scope="col">Ends (UTC)</th>
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
