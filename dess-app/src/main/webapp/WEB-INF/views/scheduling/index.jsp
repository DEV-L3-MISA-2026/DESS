<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Scheduling"/>
<c:set var="topbarContext" value="Rooms and bookings"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="pageDescription" value="Rooms and the bookings that reserve them. A booking appears as active only after the scheduling service has stored it. The weekly timetable is not available yet."/>
<c:set var="breadcrumbParentLabel" value="Overview"/>
<c:set var="breadcrumbParentHref" value="${ctx}/app"/>
<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="rooms-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="rooms-title">Rooms</h2>
            <p class="panel-note"><c:out value="${fn:length(rooms)}"/>
                <c:out value="${fn:length(rooms) == 1 ? 'room' : 'rooms'}"/>.</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty rooms}">
            <c:set var="emptyTitle" value="No room yet."/>
            <c:set var="emptyMessage" value="Add a room before a teaching session or an examination can use it."/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Rooms">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Code</th>
                        <th scope="col">Name</th>
                        <th scope="col">Capacity</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="room" items="${rooms}">
                        <tr>
                            <td class="mono"><c:out value="${room.code}"/></td>
                            <td><c:out value="${room.name}"/></td>
                            <td><c:out value="${room.capacity}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${canWrite}">
        <h3 class="form-section-title">Add a room</h3>
        <c:if test="${not empty roomErrors.form}">
            <div class="flash error" role="alert"><c:out value="${roomErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${roomErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/scheduling" class="form-row">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createRoom">
            <div class="field">
                <label for="roomCode">Code <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="roomCode" name="code" type="text" required
                       value="<c:out value='${roomForm.code}'/>"
                       <c:if test="${not empty roomErrors.roomCode}">aria-invalid="true" aria-describedby="roomCode-error"</c:if>>
                <c:if test="${not empty roomErrors.roomCode}">
                    <p class="field-error" id="roomCode-error"><c:out value="${roomErrors.roomCode}"/></p>
                </c:if>
            </div>
            <div class="field grow">
                <label for="roomName">Name <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="roomName" name="name" type="text" required
                       value="<c:out value='${roomForm.name}'/>"
                       <c:if test="${not empty roomErrors.roomName}">aria-invalid="true" aria-describedby="roomName-error"</c:if>>
                <c:if test="${not empty roomErrors.roomName}">
                    <p class="field-error" id="roomName-error"><c:out value="${roomErrors.roomName}"/></p>
                </c:if>
            </div>
            <div class="field field-small">
                <label for="roomCapacity">Capacity <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="roomCapacity" name="capacity" type="number" min="1" step="1" required
                       value="<c:out value='${roomForm.capacity}'/>"
                       <c:if test="${not empty roomErrors.roomCapacity}">aria-invalid="true" aria-describedby="roomCapacity-error"</c:if>>
                <c:if test="${not empty roomErrors.roomCapacity}">
                    <p class="field-error" id="roomCapacity-error"><c:out value="${roomErrors.roomCapacity}"/></p>
                </c:if>
            </div>
            <div class="form-actions compact">
                <button class="btn" type="submit">Add room</button>
            </div>
        </form>
    </c:if>
</section>

<section class="panel" aria-labelledby="bookings-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="bookings-title">Bookings</h2>
            <p class="panel-note">Newest first. Times are in UTC. A cancelled booking keeps its record and time range.</p>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty bookings}">
            <c:set var="emptyTitle" value="No booking yet."/>
            <c:set var="emptyMessage" value="Teaching sessions and examinations will appear here once they are scheduled."/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Bookings">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Owner</th>
                        <th scope="col">Reserved resources</th>
                        <th scope="col">Starts (UTC)</th>
                        <th scope="col">Ends (UTC)</th>
                        <th scope="col">Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="booking" items="${bookings}">
                        <tr>
                            <td><c:out value="${booking.ownerType}"/></td>
                            <td><c:out value="${booking.resources}"/></td>
                            <td class="mono"><c:out value="${booking.startsAt}"/></td>
                            <td class="mono"><c:out value="${booking.endsAt}"/></td>
                            <td>
                                <c:set var="badgeTone" value="${booking.status == 'ACTIVE' ? 'ok' : 'off'}"/>
                                <c:set var="badgeLabel" value="${booking.status == 'ACTIVE' ? 'Active' : 'Cancelled'}"/>
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
