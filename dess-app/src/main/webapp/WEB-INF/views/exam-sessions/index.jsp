<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/components/head.jspf" %>

<%@ include file="/WEB-INF/views/components/page-header.jspf" %>

<section class="panel" aria-labelledby="sessions-title">
    <div class="panel-head">
        <div>
            <h2 class="section-title" id="sessions-title">Exam sessions</h2>
            <p class="panel-note"><c:out value="${page.countLabel}"/></p>
        </div>
    </div>

    <form method="get" action="${ctx}/app/exam-sessions" class="row" role="search">
        <div class="field grow">
            <label for="sessionSearch">Search by session id</label>
            <input id="sessionSearch" name="q" type="search"
                   value="<c:out value='${page.query}'/>"
                   placeholder="Type part or all of the id">
        </div>
        <div class="form-actions compact">
            <button class="btn" type="submit">Search</button>
            <c:if test="${not empty page.query}">
                <a class="btn btn-ghost" href="${ctx}/app/exam-sessions">Clear</a>
            </c:if>
        </div>
    </form>
    <p class="hint">The match is case-insensitive and does not need the full id.</p>

    <c:choose>
        <c:when test="${empty page.sessions}">
            <c:set var="emptyState" value="${page.sessionsEmpty}"/>
            <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
        </c:when>
        <c:otherwise>
            <div class="table-wrap" tabindex="0" role="region" aria-label="Exam sessions">
                <table>
                    <thead>
                    <tr>
                        <th scope="col">Session id</th>
                        <th scope="col">Assessment</th>
                        <th scope="col">Starts (UTC)</th>
                        <th scope="col">Ends (UTC)</th>
                        <th scope="col">Room</th>
                        <th scope="col">Candidates</th>
                        <th scope="col">Invigilators</th>
                        <th scope="col">Booking</th>
                        <th scope="col">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="session" items="${page.sessions}">
                        <tr>
                            <td class="mono"><c:out value="${session.id}"/></td>
                            <td><c:out value="${session.assessment}"/></td>
                            <td class="mono"><c:out value="${session.startsAt}"/></td>
                            <td class="mono"><c:out value="${session.endsAt}"/></td>
                            <td><c:out value="${session.room}"/></td>
                            <td><c:out value="${session.candidateCount}"/></td>
                            <td><c:out value="${session.invigilators}"/></td>
                            <td>
                                <c:set var="badge" value="${session.booking}"/>
                                <%@ include file="/WEB-INF/views/components/status-badge.jspf" %>
                            </td>
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
        <c:if test="${not empty page.sessionErrors.form}">
            <div class="flash error" role="alert"><c:out value="${page.sessionErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${page.sessionErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <form method="post" action="${ctx}/app/exam-sessions" class="grid">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <input type="hidden" name="action" value="createSession">
            <div class="field">
                <label for="assessmentId">Assessment <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="assessmentId" name="assessmentId" type="text" required
                       list="assessment-options"
                       value="<c:out value='${page.sessionForm.assessmentId}'/>"
                       <c:if test="${not empty page.sessionErrors.assessmentId}">aria-invalid="true" aria-describedby="assessmentId-error"</c:if>>
                <datalist id="assessment-options">
                    <c:forEach var="assessment" items="${page.assessments}">
                        <option value="<c:out value='${assessment.value}'/>"><c:out value="${assessment.label}"/></option>
                    </c:forEach>
                </datalist>
                <p class="hint">Pick a known assessment or paste its id. The title is shown
                in the session list; an id with no definition is marked as unknown.</p>
                <c:if test="${not empty page.sessionErrors.assessmentId}">
                    <p class="field-error" id="assessmentId-error"><c:out value="${page.sessionErrors.assessmentId}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="roomId">Room <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <select id="roomId" name="roomId" required
                        <c:if test="${not empty page.sessionErrors.roomId}">aria-invalid="true" aria-describedby="roomId-error"</c:if>>
                    <option value="">Choose a room</option>
                    <c:forEach var="room" items="${page.rooms}">
                        <option value="${room.value}" ${room.selectedAttribute}><c:out value="${room.label}"/></option>
                    </c:forEach>
                </select>
                <c:if test="${not empty page.sessionErrors.roomId}">
                    <p class="field-error" id="roomId-error"><c:out value="${page.sessionErrors.roomId}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="startsAt">Starts (UTC) <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="startsAt" name="startsAt" type="datetime-local" required
                       value="<c:out value='${page.sessionForm.startsAt}'/>"
                       <c:if test="${not empty page.sessionErrors.startsAt}">aria-invalid="true" aria-describedby="startsAt-error"</c:if>>
                <c:if test="${not empty page.sessionErrors.startsAt}">
                    <p class="field-error" id="startsAt-error"><c:out value="${page.sessionErrors.startsAt}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="endsAt">Ends (UTC) <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                <input id="endsAt" name="endsAt" type="datetime-local" required
                       value="<c:out value='${page.sessionForm.endsAt}'/>"
                       <c:if test="${not empty page.sessionErrors.endsAt}">aria-invalid="true" aria-describedby="endsAt-error"</c:if>>
                <c:if test="${not empty page.sessionErrors.endsAt}">
                    <p class="field-error" id="endsAt-error"><c:out value="${page.sessionErrors.endsAt}"/></p>
                </c:if>
            </div>
            <div class="field">
                <label for="invigilators">Invigilators</label>
                <input id="invigilators" name="invigilators" type="text"
                       value="<c:out value='${page.sessionForm.invigilators}'/>"
                       <c:if test="${not empty page.sessionErrors.invigilators}">aria-invalid="true" aria-describedby="invigilators-error"</c:if>>
                <p class="hint">Comma-separated person ids. Each invigilator is also booked,
                so a person booked at the same time is reported as a conflict.</p>
                <c:if test="${not empty page.sessionErrors.invigilators}">
                    <p class="field-error" id="invigilators-error"><c:out value="${page.sessionErrors.invigilators}"/></p>
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
        <c:if test="${not empty page.invigilatorErrors.form}">
            <div class="flash error" role="alert"><c:out value="${page.invigilatorErrors.form}"/></div>
        </c:if>
        <c:set var="validationErrors" value="${page.invigilatorErrors}"/>
        <%@ include file="/WEB-INF/views/components/validation-summary.jspf" %>
        <c:choose>
            <c:when test="${empty page.selectableSessions}">
                <c:set var="emptyState" value="${page.selectableSessionsEmpty}"/>
                <%@ include file="/WEB-INF/views/components/empty-state.jspf" %>
            </c:when>
            <c:otherwise>
                <form method="post" action="${ctx}/app/exam-sessions" class="grid">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="assignInvigilators">
                    <div class="field">
                        <label for="sessionId">Session <span class="req" aria-hidden="true">*</span><span class="visually-hidden"> (required)</span></label>
                        <select id="sessionId" name="sessionId" required
                                <c:if test="${not empty page.invigilatorErrors.sessionId}">aria-invalid="true" aria-describedby="sessionId-error"</c:if>>
                            <option value="">Choose a session</option>
                            <c:forEach var="session" items="${page.selectableSessions}">
                                <option value="${session.value}" ${session.selectedAttribute}><c:out value="${session.label}"/></option>
                            </c:forEach>
                        </select>
                        <c:if test="${not empty page.invigilatorErrors.sessionId}">
                            <p class="field-error" id="sessionId-error"><c:out value="${page.invigilatorErrors.sessionId}"/></p>
                        </c:if>
                    </div>
                    <div class="field">
                        <label for="assignInvigilators">Invigilator person ids</label>
                        <input id="assignInvigilators" name="invigilators" type="text"
                               value="<c:out value='${page.invigilatorForm.invigilators}'/>"
                               <c:if test="${not empty page.invigilatorErrors.invigilators}">aria-invalid="true" aria-describedby="assignInvigilators-error"</c:if>>
                        <p class="hint">Comma-separated person ids. Leave empty to keep only
                        the room booked.</p>
                        <c:if test="${not empty page.invigilatorErrors.invigilators}">
                            <p class="field-error" id="assignInvigilators-error"><c:out value="${page.invigilatorErrors.invigilators}"/></p>
                        </c:if>
                    </div>
                    <div class="form-actions span-2">
                        <button class="btn" type="submit">Update invigilators</button>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </section>

    <c:if test="${not empty page.cancelErrors}">
        <div class="flash error" role="alert">
            <strong>The session was not cancelled.</strong>
            <ul>
                <c:forEach var="entry" items="${page.cancelErrors}">
                    <li><c:out value="${entry.value}"/></li>
                </c:forEach>
            </ul>
        </div>
    </c:if>

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
