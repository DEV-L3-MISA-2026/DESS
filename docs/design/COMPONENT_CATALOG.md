# DESS Component Catalog

This catalog defines the shared UI building blocks. Implement shared components before duplicating patterns across feature pages.

## 1. Application shell

### Sidebar
Contains the DESS identity and primary navigation. Shows active location and permission-appropriate links. On small screens, becomes a drawer. Links and buttons in the sidebar use the light focus ring from `--dess-focus-ring-on-dark`.

### Top navbar
Contains page-level global controls such as current institution/context, user menu, and optional notifications. Avoid placing every page action in the global navbar.

### Breadcrumb
Shows the current location in the information hierarchy. Use links for navigable ancestors and plain text for the current page. Breadcrumb links are muted and underline on hover.

## 2. Interaction states

Every interactive component defines the same states. Do not ship a component with only a hover state.

| State | Requirement |
|---|---|
| Default | Token colours, visible border or background |
| Hover | Small colour change, never a layout shift, `--dess-transition-fast` |
| Active/pressed | Clearly darker or tinted; buttons keep their own colour family |
| Focus-visible | Two-layer ring; light ring on dark surfaces |
| Disabled | Reduced opacity, `cursor: not-allowed`, no hover effect |
| Invalid | `--dess-danger` border plus a message; never colour alone |
| Selected/current | Accent border or `--dess-accent-soft` background plus `aria-current` or `aria-pressed` |

Controls are 40px high on a mouse and at least 44px on a coarse pointer.

## 3. Buttons and actions

### Button hierarchy
- Primary: accent background, one per region.
- Secondary: white surface with a border.
- Destructive: danger fill, separated from frequent actions, always confirmed.
- Quiet: text or icon-only action in toolbars and table rows.

Icon-only buttons need an accessible name. Prefer an icon with text when the meaning is not universal.

## 4. Page components

### Page header
- Breadcrumb (optional)
- Title (required)
- Description (optional)
- Primary action (optional)
- Secondary actions (optional)

The application pages render the header, breadcrumb, empty state and status badge
from shared JSP fragments under `WEB-INF/views/components/`: `page-header.jspf`,
`breadcrumb.jspf`, `empty-state.jspf` and `status-badge.jspf`. Pages set EL variables
and include the fragment; they do not repeat the markup.

### Section header
A small heading and optional supporting action used to organize long pages.

### Panel
A restrained white surface with a subtle border. Use for grouping related content, not as a wrapper around every element.

`.panel` is the shared class for application pages. The institution editor still uses
`.card`; both render the same surface, padding, radius and shadow, so they cannot drift
apart. Only the stacking margin differs.

## 5. Data components

### Data table
Required capabilities depend on the page:
- Clear column labels.
- Consistent row spacing and left-aligned text.
- Optional sorting, with `aria-sort` on the header cell.
- Optional search and filters.
- Row-level action menu where appropriate.
- Pagination for large datasets.
- Empty state.
- Loading and error state when data is fetched asynchronously.

Wrap the table in `.table-wrap`. Give the wrapper `tabindex="0"` and an accessible name so keyboard users can scroll it horizontally on narrow screens. Do not use a checkbox column unless bulk actions are actually supported.

### Pagination
Show current page, total pages or result count, previous/next controls, and page size only when useful.

### Empty state
Explain what data is absent and provide a relevant next action when the user has permission to create it. Avoid decorative illustrations that consume space.

### Filter bar
Keep frequently used filters visible. Put infrequent/advanced filters behind a clearly labeled control. Make active filters apparent and easy to clear.

## 6. Form components

### Text field
Label, optional required indicator, input, hint, and field-level error. Use `.hint` for guidance and `.field-error` for the message. Native `:user-invalid` and `aria-invalid="true"` both show a danger border; always render the message in text as well.

### Select field
Use for a bounded set of known choices. For large datasets, use searchable selection rather than a very long native select.

### Date/time fields
Use explicit labels and unambiguous date/time formats. State the timezone if the deployment context makes it necessary.

### Form section
Group related fields under a meaningful heading. Avoid one giant undifferentiated form.

### Form actions
Primary submit action, secondary cancel/back action, and destructive action separated visually.

## 7. Feedback components

### Alert
Variants: success, info, warning, danger. Each alert has a concise title or message and actionable guidance when relevant. Success, info and warning use a soft background with a 4px left border; danger uses the same structure with `--dess-danger-soft`. Include the outcome in the text so the colour is not the only signal.

### Status badge
Variants: draft, pending, confirmed, conflict, inactive, cancelled. Always include text. The inactive state uses a hollow dot so it differs by shape as well as colour.

### Confirmation dialog
Used for irreversible or consequential actions. State the object and consequence explicitly. Destructive action must not be the default focused action. Use a native `<dialog>`: it traps focus, closes on Escape, and returns focus to the control that opened it. Use the shared head, body and foot parts.

### Validation summary
Summarizes form errors and links/focuses the corresponding fields where practical. Implemented as `validation-summary.jspf`. Servlets use the field's DOM id as the error-map key, so each message links to `#<key>`. A `form` key is reserved for a form-level message and stays in the page's own alert.

## 8. Scheduling components

### Timetable toolbar
Shows the selected view (group, teacher, room), date range, navigation controls, and relevant filters.

### Timetable event
Shows a concise title, time, and key resource label. Details can appear in a popover or detail panel.

### Conflict notice
Must state:
- Which resource conflicts (room, teacher, group).
- The overlapping date/time.
- The conflicting session, when the current user is authorized to see it.
- A suggested correction, when one can be safely inferred.

### Mobile schedule list
Chronological list grouped by day, preserving the same information as the calendar.

## 9. Naming and implementation

Suggested JSP fragments/tag files:
```text
WEB-INF/views/components/
├── head.jspf
├── sidebar.jsp
├── topbar.jsp
├── breadcrumb.jspf
├── page-header.jspf
├── flash.jsp
├── status-badge.jspf
├── empty-state.jspf
└── validation-summary.jspf
```

### Page view models

Application pages expose one `page` request attribute built under
`com.dess.platform.web.view`. A page view carries the shell (`PageChrome`), the
count labels, the empty states, the badges and typed rows. The JSP reads
`${page.*}` and keeps markup only; it does not build labels or choose badge
tones.

`Views.render` also sets the flat shell attributes, so the institution editor,
login and error pages keep working. Hrefs inside a view model are
application-relative. `Views.render` adds the context path for shell values and
`empty-state.jspf` adds it for empty-state actions.

Suggested CSS organization:
- `tokens.css`: canonical variables.
- `layout.css`: shell, sidebar, navbar, content layout.
- `components.css`: reusable controls and patterns.
- `pages.css`: narrowly scoped page-specific layout.

Do not create a shared component that hides fundamentally different behavior behind many boolean flags. Prefer a small set of composable, understandable components.
