<%-- Shared top bar. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<header class="dess-topbar">
    <div class="dess-topbar-context">
        <button type="button" class="dess-mobile-menu" data-nav-toggle
                aria-expanded="false" aria-controls="dess-sidebar">Menu</button>
        <span class="dess-topbar-title"><c:out value="${pageTitle}"/></span>
        <c:if test="${not empty topbarContext}">
            <span class="dess-topbar-separator" aria-hidden="true">/</span>
            <span><c:out value="${topbarContext}"/></span>
        </c:if>
    </div>
    <div class="dess-topbar-right">
        <%@ include file="/WEB-INF/views/components/user-menu.jspf" %>
    </div>
</header>
