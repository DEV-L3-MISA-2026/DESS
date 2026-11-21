<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Manage exam sessions"/>
<c:set var="topbarContext" value="Examinations"/>
<%@ include file="/WEB-INF/views/components/head.jspf" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<div class="dess-page-header">
    <div class="dess-page-heading">
        <nav class="dess-breadcrumb" aria-label="Breadcrumb">
            <ol>
                <li><a href="${ctx}/app">Overview</a></li>
                <li><a href="${ctx}/app/examinations">Examinations</a></li>
                <li aria-current="page">Manage sessions</li>
            </ol>
        </nav>
        <h1 class="dess-page-title">Manage exam sessions</h1>
        <p class="dess-page-description">
            Schedule sessions, find one by its id, change its invigilators or cancel it. A
            session is stored only after the booking service confirmed the room and the
            invigilators for that time range. Times are entered and shown in UTC.
        </p>
    </div>
</div>

<section class="panel" aria-labelledby="sessions-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="sessions-title">Exam sessions</h2>
            <p class="panel-note"><c:out value="${fn:length(sessions)}"/> of
                <c:out value="${fn:length(selectableSessions)}"/> session(s) shown.</p>
        </div>
    </div>

    <form method="get" action="${ctx}/app/exam-sessions" class="row" role="search">
        <div class="field grow">
            <label for="sessionSearch">Search by session id</label>
            <input id="sessionSearch" name="q" type="search"
                   value="<c:out value='${query}'/>"
                   placeholder="Type part or all of the id">
        </div>
        <div class="form-actions compact">
            <button class="btn" type="submit">Search</button>
            <c:if test="${not empty query}">
                <a class="btn btn-ghost" href="${ctx}/app/exam-sessions">Clear</a>
            </c:if>
        </div>
    </form>
    <p class="hint">The match is case-insensitive and does not need the full id.</p>

    <c:choose>
        <c:when test="${empty sessions}">
            <div class="empty">
                <c:choose>
                    <c:when test="${not empty query}">
                        <strong>No session matches this id.</strong>
                        <p>Try a shorter id or <a href="${ctx}/app/exam-sessions">clear the search</a>.</p>
                    </c:when>
                    <c:otherwise>
                        <strong>No exam session yet.</strong>
                        <p>Schedule the first session below.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Exam sessions">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Session id</th>
                        <th scope="col">Starts</th>
                        <th scope="col">Ends</th>
                        <th scope="col">Room</th>
                        <th scope="col">Candidates</th>
                        <th scope="col">Invigilators</th>
                        <th scope="col">Booking</th>
                        <th scope="col">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="session" items="${sessions}">
                        <tr>
                            <td class="mono"><c:out value="${session.id}"/></td>
                            <td class="mono"><c:out value="${session.startsAt}"/></td>
                            <td class="mono"><c:out value="${session.endsAt}"/></td>
                            <td><c:out value="${session.room}"/></td>
                            <td><c:out value="${session.candidateCount}"/></td>
                            <td><c:out value="${session.invigilators}"/></td>
                            <td><span class="badge ok">Confirmed</span></td>
                            <td>
                                <c:if test="${canWrite}">
                                    <button type="button" class="btn btn-danger btn-sm"
                                            data-cancel-session="<c:out value='${session.id}'/>">
                                        Cancel
                                    </button>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>

    <c:if test="${canWrite}">
        <h3 class="form-section-title">Schedule an exam session</h3>
        <c:if test="${not empty sessionErrors.form}">
            <div class="flash error" role="alert"><c:out value="${sessionErrors.form}"/></div>
        </c:if>
        <form method="post" action="${ctx}/app/exam-sessions" class="grid">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createSession">
            <div class="field">
                <label for="assessmentId">Assessment id <span class="req" aria-hidden="true">*</span></label>
                <input id="assessmentId" name="assessmentId" type="text" required
                       value="<c:out value='${sessionForm.assessmentId}'/>"
                       <c:if test="${not empty sessionErrors.assessmentId}">aria-invalid="true" aria-describedby="assessmentId-error"</c:if>>
                <p class="hint">UUID of the assessment definition. The assessment catalogue
                is not implemented yet.</p>
                <c:if test="${not empty sessionErrors.assessmentId}">
                    <p class="field-error" id="assessmentId-error"><c:out value="${sessionErrors.assessmentId}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="roomId">Room <span class="req" aria-hidden="true">*</span></label>
                <select id="roomId" name="roomId" required
                        <c:if test="${not empty sessionErrors.roomId}">aria-invalid="true" aria-describedby="roomId-error"</c:if>>
                    <option value="">Choose a room</option>
                    <c:forEach var="room" items="${rooms}">
                        <option value="<c:out value='${room.id}'/>"
                                <c:if test="${sessionForm.roomId == room.id}">selected</c:if>>
                            <c:out value="${room.label}"/>
                        </option>
                    </c:forEach>
                </select>
                <c:if test="${not empty sessionErrors.roomId}">
                    <p class="field-error" id="roomId-error"><c:out value="${sessionErrors.roomId}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="startsAt">Starts (UTC) <span class="req" aria-hidden="true">*</span></label>
                <input id="startsAt" name="startsAt" type="datetime-local" required
                       value="<c:out value='${sessionForm.startsAt}'/>"
                       <c:if test="${not empty sessionErrors.startsAt}">aria-invalid="true" aria-describedby="startsAt-error"</c:if>>
                <c:if test="${not empty sessionErrors.startsAt}">
                    <p class="field-error" id="startsAt-error"><c:out value="${sessionErrors.startsAt}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="endsAt">Ends (UTC) <span class="req" aria-hidden="true">*</span></label>
                <input id="endsAt" name="endsAt" type="datetime-local" required
                       value="<c:out value='${sessionForm.endsAt}'/>"
                       <c:if test="${not empty sessionErrors.endsAt}">aria-invalid="true" aria-describedby="endsAt-error"</c:if>>
                <c:if test="${not empty sessionErrors.endsAt}">
                    <p class="field-error" id="endsAt-error"><c:out value="${sessionErrors.endsAt}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="invigilators">Invigilators</label>
                <input id="invigilators" name="invigilators" type="text"
                       value="<c:out value='${sessionForm.invigilators}'/>"
                       <c:if test="${not empty sessionErrors.invigilators}">aria-invalid="true" aria-describedby="invigilators-error"</c:if>>
                <p class="hint">Comma-separated person ids. Each invigilator is also booked,
                so a person booked at the same time is reported as a conflict.</p>
                <c:if test="${not empty sessionErrors.invigilators}">
                    <p class="field-error" id="invigilators-error"><c:out value="${sessionErrors.invigilators}"/></p>
                </c:if>
            </div>
            <div class="form-actions span-2">
                <button class="btn" type="submit">Schedule session</button>
            </div>
        </form>
    </c:if>
</section>

<c:if test="${canWrite}">
    <section class="panel" aria-labelledby="invigilators-title">
        <div class="panel-head">
            <div>
                <h2 class="section-title" id="invigilators-title">Assign invigilators</h2>
                <p class="panel-note">Replaces the current invigilators and their reservations.
                The room and time range stay the same.</p>
            </div>
        </div>
        <c:if test="${not empty invigilatorErrors.form}">
            <div class="flash error" role="alert"><c:out value="${invigilatorErrors.form}"/></div>
        </c:if>
        <c:choose>
            <c:when test="${empty selectableSessions}">
                <div class="empty">
                    <strong>No session to update.</strong>
                    <p>Schedule a session first.</p>
                </div>
            </c:when>
            <c:otherwise>
                <form method="post" action="${ctx}/app/exam-sessions" class="grid">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="assignInvigilators">
                    <div class="field">
                        <label for="sessionId">Session <span class="req" aria-hidden="true">*</span></label>
                        <select id="sessionId" name="sessionId" required
                                <c:if test="${not empty invigilatorErrors.sessionId}">aria-invalid="true" aria-describedby="sessionId-error"</c:if>>
                            <option value="">Choose a session</option>
                            <c:forEach var="session" items="${selectableSessions}">
                                <option value="<c:out value='${session.id}'/>"
                                        <c:if test="${invigilatorForm.sessionId == session.id}">selected</c:if>>
                                    <c:out value="${session.startsAt}"/> - <c:out value="${session.room}"/>
                                </option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty invigilatorErrors.sessionId}">
                            <p class="field-error" id="sessionId-error"><c:out value="${invigilatorErrors.sessionId}"/></p>
                        </c:if>
                    </div>
                    <div class="field">
                        <label for="assignInvigilators">Invigilator person ids</label>
                        <input id="assignInvigilators" name="invigilators" type="text"
                               value="<c:out value='${invigilatorForm.invigilators}'/>"
                               <c:if test="${not empty invigilatorErrors.invigilators}">aria-invalid="true" aria-describedby="assignInvigilators-error"</c:if>>
                        <p class="hint">Comma-separated person ids. Leave empty to keep only
                        the room booked.</p>
                        <c:if test="${not empty invigilatorErrors.invigilators}">
                            <p class="field-error" id="assignInvigilators-error"><c:out value="${invigilatorErrors.invigilators}"/></p>
                        </c:if>
                    </div>
                    <div class="form-actions span-2">
                        <button class="btn" type="submit">Update invigilators</button>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </section>

    <dialog id="cancel-session-dialog" aria-labelledby="cancel-session-title">
        <div class="dialog-form">
            <div class="dialog-head">
                <div>
                    <h2 id="cancel-session-title">Cancel exam session</h2>
                    <p>This releases the booking and removes the session.</p>
                </div>
            </div>
            <div class="dialog-body">
                <p>Session id: <span class="mono" id="cancel-session-label"></span></p>
                <p class="hint">This cannot be undone. The room and the invigilators become
                available again for the same time range.</p>
            </div>
            <div class="dialog-foot">
                <button type="button" class="btn btn-ghost" data-close-dialog>Keep session</button>
                <form method="post" action="${ctx}/app/exam-sessions">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="cancelSession">
                    <input type="hidden" name="sessionId" id="cancel-session-id" value="">
                    <button type="submit" class="btn btn-danger">Cancel session</button>
                </form>
            </div>
        </div>
    </dialog>
</c:if>

<script>
(function () {
    var dialog = document.getElementById('cancel-session-dialog');
    if (!dialog) return;

    var idField = document.getElementById('cancel-session-id');
    var label = document.getElementById('cancel-session-label');

    Array.prototype.forEach.call(document.querySelectorAll('[data-cancel-session]'), function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-cancel-session');
            idField.value = id;
            label.textContent = id;
            dialog.showModal();
        });
    });

    Array.prototype.forEach.call(dialog.querySelectorAll('[data-close-dialog]'), function (button) {
        button.addEventListener('click', function () { dialog.close(); });
    });
})();
</script>

<%@ include file="/WEB-INF/views/components/foot.jspf" %>
