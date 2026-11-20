<%--
    Shared application sidebar.

    The fragment is included statically by server-rendered pages. Only the
    Institution structure page exists today; the other sections are marked
    "Soon" and are intentionally not links.
--%>
<aside class="dess-sidebar" id="dess-sidebar" aria-label="Application sidebar">
    <div class="dess-sidebar-header">
        <div class="dess-sidebar-header-row">
            <a class="dess-brand" href="${pageContext.request.contextPath}/">
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
                <a class="dess-nav-link" href="${pageContext.request.contextPath}/"
                   aria-current="page">Institution structure</a>
            </li>
            <li>
                <span class="dess-nav-link is-disabled" aria-disabled="true">
                    People
                    <span class="dess-nav-soon">Soon</span>
                </span>
            </li>
        </ul>

        <p class="dess-nav-heading">Academic operations</p>
        <ul class="dess-nav-list">
            <li>
                <span class="dess-nav-link is-disabled" aria-disabled="true">
                    Academic
                    <span class="dess-nav-soon">Soon</span>
                </span>
            </li>
            <li>
                <span class="dess-nav-link is-disabled" aria-disabled="true">
                    Scheduling
                    <span class="dess-nav-soon">Soon</span>
                </span>
            </li>
            <li>
                <span class="dess-nav-link is-disabled" aria-disabled="true">
                    Assessment
                    <span class="dess-nav-soon">Soon</span>
                </span>
            </li>
        </ul>

        <p class="dess-nav-heading">Administration</p>
        <ul class="dess-nav-list">
            <li>
                <span class="dess-nav-link is-disabled" aria-disabled="true">
                    Security
                    <span class="dess-nav-soon">Soon</span>
                </span>
            </li>
        </ul>
    </nav>

    <div class="dess-sidebar-footer">
        <p>Institution module</p>
        <p>Local development only</p>
    </div>
</aside>

<button type="button" class="dess-scrim" data-nav-close tabindex="-1"
        aria-hidden="true" hidden></button>
