<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Overview"/>
<c:set var="topbarContext" value="Academic operations"/>
<c:set var="pageDescription" value="Current counts and the next active bookings. Each figure links to the list it summarises."/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section aria-labelledby="counts-title">
    <h2 class="section-title" id="counts-title">Current data</h2>
    <ul class="stat-grid">
        <li class="stat-card">
            <a class="stat-link" href="${ctx}/app/people">
                <span class="stat-label">Students</span>
                <span class="stat-value"><c:out value="${studentCount}"/></span>
                <span class="stat-note">People directory</span>
            </a>
        </li>
        <li class="stat-card">
            <a class="stat-link" href="${ctx}/app/academic">
                <span class="stat-label">Courses</span>
                <span class="stat-value"><c:out value="${courseCount}"/></span>
                <span class="stat-note">Course catalogue</span>
            </a>
        </li>
        <li class="stat-card">
            <a class="stat-link" href="${ctx}/app/academic">
                <span class="stat-label">Periods</span>
                <span class="stat-value"><c:out value="${periodCount}"/></span>
                <span class="stat-note">Academic calendar</span>
            </a>
        </li>
        <li class="stat-card">
            <a class="stat-link" href="${ctx}/app/academic">
                <span class="stat-label">Programs</span>
                <span class="stat-value"><c:out value="${programCount}"/></span>
                <span class="stat-note">Programmes and versions</span>
            </a>
        </li>
        <li class="stat-card">
            <a class="stat-link" href="${ctx}/app/scheduling">
                <span class="stat-label">Rooms</span>
                <span class="stat-value"><c:out value="${roomCount}"/></span>
                <span class="stat-note">Schedulable spaces</span>
            </a>
        </li>
        <li class="stat-card">
            <a class="stat-link" href="${ctx}/app/scheduling">
                <span class="stat-label">Bookings</span>
                <span class="stat-value"><c:out value="${bookingCount}"/></span>
                <span class="stat-note">Active and cancelled</span>
            </a>
        </li>
        <c:if test="${sessionUser.admin}">
            <li class="stat-card">
                <a class="stat-link" href="${ctx}/app/security">
                    <span class="stat-label">Accounts</span>
                    <span class="stat-value"><c:out value="${accountCount}"/></span>
                    <span class="stat-note">Sign-in accounts</span>
                </a>
            </li>
        </c:if>
    </ul>
</section>

<section class="panel" aria-labelledby="bookings-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="bookings-title">Upcoming bookings</h2>
            <p class="panel-note">The next six active bookings, earliest first. Times are in UTC.</p>
        </div>
        <a class="btn btn-sm" href="${ctx}/app/scheduling">Open scheduling</a>
    </div>

    <c:choose>
        <c:when test="${empty upcomingBookings}">
            <c:set var="emptyTitle" value="No upcoming booking."/>
            <c:set var="emptyMessage" value="Active bookings that start from now on appear here once the scheduling service stores them."/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Upcoming bookings">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Owner</th>
                        <th scope="col">Reserved resources</th>
                        <th scope="col">Starts (UTC)</th>
                        <th scope="col">Ends (UTC)</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="booking" items="${upcomingBookings}">
                        <tr>
                            <td><c:out value="${booking.ownerType}"/></td>
                            <td><c:out value="${booking.resources}"/></td>
                            <td class="mono"><c:out value="${booking.startsAt}"/></td>
                            <td class="mono"><c:out value="${booking.endsAt}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</section>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
