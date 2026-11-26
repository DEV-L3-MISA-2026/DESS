<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section aria-labelledby="counts-title">
    <h2 class="section-title" id="counts-title">Current data</h2>
    <ul class="stat-grid">
        <c:forEach var="card" items="${page.stats}">
            <li class="stat-card">
                <a class="stat-link" href="${ctx}${card.href}">
                    <span class="stat-label"><c:out value="${card.label}"/></span>
                    <span class="stat-value"><c:out value="${card.value}"/></span>
                    <span class="stat-note"><c:out value="${card.note}"/></span>
                </a>
            </li>
        </c:forEach>
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
        <c:when test="${empty page.bookings}">
            <c:set var="emptyState" value="${page.bookingsEmpty}"/>
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
                    <c:forEach var="booking" items="${page.bookings}">
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
