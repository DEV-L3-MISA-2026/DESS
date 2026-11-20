# DESS UX Rules and Acceptance Checklist

## General interaction rules
- Every page must answer: where am I, what can I do here, and what happens next?
- Use consistent names for the same domain concept throughout the application.
- Keep primary actions in a predictable location.
- Avoid destructive actions beside frequent actions without separation.
- Do not use placeholder text as the only label.
- Do not use color as the only signal for status, errors, or selection.
- Avoid unexpected navigation after validation errors.
- Confirm consequential actions and identify the affected entity.

## Accessibility
- Use semantic landmarks (`header`, `nav`, `main`, `footer`) where appropriate.
- Maintain a logical heading hierarchy.
- Associate every form control with a label.
- Ensure keyboard access to all actions.
- Show a visible focus indicator: a 2px solid edge with a 2px surface gap, at least 3:1 against the surrounding colours. Use the light ring on the sidebar.
- Provide accessible names for icon-only controls.
- Use text/background contrast of at least 4.5:1 for normal text and 3:1 for large text and interface boundaries.
- Do not use colour as the only signal for a status, an error or a selection; add text, a shape or an icon.
- Keep interactive targets at least 44px on touch screens where practical.
- Respect `prefers-reduced-motion`; do not animate for decoration.
- Announce important asynchronous status changes to assistive technology where applicable.
- Do not trap keyboard focus outside a modal while it is open.

## Data integrity and trust
- Display dates and times unambiguously.
- Show the academic period/context for academic operations.
- Distinguish inactive from deleted.
- Do not suggest an action succeeded until the server confirms it.
- Show server-side validation failures in a useful, actionable form.
- Prevent double submission where duplicate operations would be harmful.
- Use CSRF protection for state-changing forms.

## Responsive acceptance
At minimum, inspect:
- Wide desktop (approximately 1440px).
- Standard laptop (approximately 1280px).
- Tablet (approximately 768px).
- Narrow mobile (approximately 375px).

Check that:
- No page-level horizontal overflow occurs.
- Navigation remains reachable.
- Forms remain usable.
- Tables have a deliberate narrow-screen behavior.
- Scheduling has a list/day alternative.
- Dialogs fit within the viewport.

## Page review checklist
- [ ] Uses canonical design tokens.
- [ ] Uses shared components where available.
- [ ] Has page title and clear context.
- [ ] Primary action is obvious.
- [ ] Every interactive element has default, hover, active, focus-visible and disabled states.
- [ ] Text contrast is at least 4.5:1; focus indicators are visible on light and dark surfaces.
- [ ] Forms have labels and useful validation.
- [ ] Empty state is meaningful.
- [ ] Error and success feedback are clear.
- [ ] Keyboard focus is visible.
- [ ] Status is not color-only.
- [ ] Responsive behavior is deliberate.
- [ ] Untrusted output is encoded.
- [ ] Server-side authorization and validation exist.
- [ ] Relevant tests/build checks pass.

## Review severity
- **Critical:** security issue, data corruption risk, inaccessible core action, or misleading confirmed scheduling state.
- **Major:** broken core workflow, missing validation, unusable responsive layout, or inconsistent interaction that can cause user error.
- **Minor:** visual inconsistency, spacing issue, unclear microcopy, or non-blocking accessibility improvement.
