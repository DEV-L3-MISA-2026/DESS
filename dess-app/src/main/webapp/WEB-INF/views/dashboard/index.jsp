<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Overview"/>
<c:set var="topbarContext" value="Academic operations"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<div class="dess-page-header">
    <div class="dess-page-heading">
        <nav class="dess-breadcrumb" aria-label="Breadcrumb">
            <ol>
                <li aria-current="page">Overview</li>
            </ol>
        </nav>
        <h1 class="dess-page-title">Overview</h1>
        <p class="dess-page-description">
            Current counts and the latest bookings. Each figure links to the list it summarises.
        </p>
    </div>
</div>

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
            <h2 class="section-title" id="bookings-title">Latest bookings</h2>
            <p class="panel-note">The six most recent records, newest first.</p>
        </div>
        <a class="btn btn-sm" href="${ctx}/app/scheduling">Open scheduling</a>
    </div>

    <c:choose>
        <c:when test="${empty recentBookings}">
            <div class="empty">
                <strong>No bookings yet.</strong>
                <p>Bookings appear here after a teaching session or another resource is scheduled.</p>
                <a class="btn" href="${ctx}/app/scheduling">Go to scheduling</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Latest bookings">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Owner</th>
                        <th scope="col">Starts</th>
                        <th scope="col">Ends</th>
                        <th scope="col">Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="booking" items="${recentBookings}">
                        <tr>
                            <td><c:out value="${booking.ownerType}"/></td>
                            <td class="mono"><c:out value="${booking.startsAt}"/></td>
                            <td class="mono"><c:out value="${booking.endsAt}"/></td>
                            <td>
                                <span class="badge ${booking.status == 'ACTIVE' ? 'ok' : 'off'}">
                                    <c:out value="${booking.status == 'ACTIVE' ? 'Active' : 'Cancelled'}"/>
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
