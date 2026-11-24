<%-- Session flash message, moved to the request by the servlet. --%>
<c:if test="${not empty flashMessage}">
    <c:set var="flashClass" value="${flashKind == 'success' ? 'ok' : (flashKind == 'warning' ? 'warn' : flashKind)}"/>
    <div class="flash ${flashClass}" role="${flashKind == 'error' ? 'alert' : 'status'}">
        <c:out value="${flashMessage}"/>
    </div>
</c:if>
