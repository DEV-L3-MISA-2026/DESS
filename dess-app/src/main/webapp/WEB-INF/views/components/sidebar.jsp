<%--
    Shared application sidebar.

    The links are permission-aware for navigation only. The server still guards every
    request; hiding a link is not a security boundary. The current item is marked with
    an EL expression inside the attribute so the markup stays valid HTML.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="active" value="${empty activeNav ? 'institution' : activeNav}"/>
<aside class="dess-sidebar" id="dess-sidebar" aria-label="Application sidebar">
    <div class="dess-sidebar-header">
        <div class="dess-sidebar-header-row">
            <a class="dess-brand" href="${ctx}/app">
                <span class="dess-brand-mark" aria-hidden="true">D</span>
                <span class="dess-brand-copy">
                    <span class="dess-brand-name">DESS</span>
                    <span class="dess-brand-sub">Department Exam and Schedule System</span>
                </span>
            </a>
            <button type="button" class="dess-sidebar-close" data-nav-close
                    aria-label="Close navigation">Close</button>
        </div>
    </div>

    <nav class="dess-nav" aria-label="Primary">
        <p class="dess-nav-heading">Workspace</p>
        <ul class="dess-nav-list">
            <li>
                <a class="dess-nav-link" href="${ctx}/app"
                   ${active == 'overview' ? 'aria-current="page"' : ''}>Overview</a>
            </li>
            <li>
                <a class="dess-nav-link" href="${ctx}/"
                   ${active == 'institution' ? 'aria-current="page"' : ''}>Institution structure</a>
            </li>
        </ul>

        <p class="dess-nav-heading">Academic operations</p>
        <ul class="dess-nav-list">
            <li>
                <a class="dess-nav-link" href="${ctx}/app/people"
                   ${active == 'people' ? 'aria-current="page"' : ''}>People</a>
            </li>
            <li>
                <a class="dess-nav-link" href="${ctx}/app/academic"
                   ${active == 'academic' ? 'aria-current="page"' : ''}>Academic</a>
            </li>
            <li>
                <a class="dess-nav-link" href="${ctx}/app/scheduling"
                   ${active == 'scheduling' ? 'aria-current="page"' : ''}>Scheduling</a>
            </li>
            <li>
                <a class="dess-nav-link" href="${ctx}/app/examinations"
                   ${active == 'examinations' ? 'aria-current="page"' : ''}>Examinations</a>
            </li>
            <li>
                <a class="dess-nav-link" href="${ctx}/app/results"
                   ${active == 'results' ? 'aria-current="page"' : ''}>Results</a>
            </li>
        </ul>

        <c:if test="${sessionUser.admin}">
            <p class="dess-nav-heading">Administration</p>
            <ul class="dess-nav-list">
                <li>
                    <a class="dess-nav-link" href="${ctx}/app/security"
                       ${active == 'security' ? 'aria-current="page"' : ''}>Security</a>
                </li>
            </ul>
        </c:if>
    </nav>

    <div class="dess-sidebar-footer">
        <p>DESS</p>
        <p>Local development only</p>
    </div>
</aside>

<button type="button" class="dess-scrim" data-nav-close tabindex="-1"
        aria-hidden="true" hidden></button>
