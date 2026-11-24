/*
 * DESS application shell behaviour.
 *
 * Keeps the mobile navigation drawer and confirmation prompts out of page
 * scripts so every server-rendered page behaves the same way.
 */
(function () {
    'use strict';

    var body = document.body;
    var toggle = document.querySelector('[data-nav-toggle]');
    var sidebar = document.getElementById('dess-sidebar');
    var main = document.querySelector('.dess-main');
    var scrim = document.querySelector('.dess-scrim');
    var closeButtons = Array.prototype.slice.call(
        document.querySelectorAll('[data-nav-close]')
    );
    var mobileQuery = window.matchMedia('(max-width: 960px)');

    function isOpen() {
        return body.classList.contains('nav-open');
    }

    function focusableItems() {
        return Array.prototype.filter.call(
            sidebar.querySelectorAll(
                'a[href], button:not([disabled]), input:not([disabled]), ' +
                'select:not([disabled]), textarea:not([disabled]), ' +
                '[tabindex]:not([tabindex="-1"])'
            ),
            function (element) {
                return element.getClientRects().length > 0;
            }
        );
    }

    function setOpen(open, returnFocus) {
        if (!toggle || !sidebar) {
            return;
        }

        body.classList.toggle('nav-open', open);
        sidebar.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');

        // While the drawer is open, take the page behind it out of the tab
        // order and the accessibility tree. Inert also blocks pointer events,
        // so the scrim is the only way to reach the page behind the drawer.
        if (main) {
            if (open) {
                main.setAttribute('inert', '');
            } else {
                main.removeAttribute('inert');
            }
        }

        if (scrim) {
            scrim.hidden = !open;
        }

        if (open) {
            var first = sidebar.querySelector('a, button');

            if (first) {
                first.focus();
            }
        } else if (returnFocus) {
            toggle.focus();
        }
    }

    if (toggle && sidebar) {
        if (scrim) {
            scrim.hidden = true;
        }

        toggle.addEventListener('click', function () {
            setOpen(!isOpen(), true);
        });

        closeButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                setOpen(false, true);
            });
        });

        sidebar.querySelectorAll('a').forEach(function (link) {
            link.addEventListener('click', function () {
                setOpen(false, false);
            });
        });

        document.addEventListener('keydown', function (event) {
            if (!isOpen()) {
                return;
            }

            if (event.key === 'Escape') {
                setOpen(false, true);
                return;
            }

            if (event.key !== 'Tab') {
                return;
            }

            var items = focusableItems();

            if (items.length === 0) {
                return;
            }

            var first = items[0];
            var last = items[items.length - 1];
            var active = document.activeElement;
            var outside = !sidebar.contains(active);

            if (event.shiftKey && (active === first || outside)) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && (active === last || outside)) {
                event.preventDefault();
                first.focus();
            }
        });

        function syncViewport() {
            if (!mobileQuery.matches && isOpen()) {
                setOpen(false, false);
            }
        }

        if (mobileQuery.addEventListener) {
            mobileQuery.addEventListener('change', syncViewport);
        } else if (mobileQuery.addListener) {
            mobileQuery.addListener(syncViewport);
        }
    }

    document.addEventListener('submit', function (event) {
        var message = event.target.getAttribute('data-confirm');

        if (message && !window.confirm(message)) {
            event.preventDefault();
        }
    });
})();
