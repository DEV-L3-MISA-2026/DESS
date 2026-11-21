<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Results"/>
<c:set var="topbarContext" value="Results by assessment"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="pageDescription" value="Each exam session with the number of results recorded for its assessment. The page never shows a mark that was not stored, and result entry is not available here yet."/>
<c:set var="breadcrumbParentLabel" value="Overview"/>
<c:set var="breadcrumbParentHref" value="${ctx}/app"/>
<c:set var="headerActionLabel" value="Open examinations"/>
<c:set var="headerActionHref" value="${ctx}/app/examinations"/>
<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="sessions-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="sessions-title">Exam sessions</h2>
            <p class="panel-note"><c:out value="${fn:length(sessions)}"/>
                <c:out value="${fn:length(sessions) == 1 ? 'session' : 'sessions'}"/>. Times are in UTC. Results are counted per assessment.</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty sessions}">
            <c:set var="emptyTitle" value="No exam session yet."/>
            <c:set var="emptyMessage" value="Results are counted per assessment once an exam session exists. Create one from the examinations page first."/>
            <c:set var="emptyActionLabel" value="Open examinations"/>
            <c:set var="emptyActionHref" value="${ctx}/app/examinations"/>
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
                                <c:set var="badgeTone" value="${session.resultTone}"/>
                                <c:set var="badgeLabel" value="${session.resultLabel}"/>
                                <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
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
