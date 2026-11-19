# DESS Component Catalog

This catalog defines the shared UI building blocks. Implement shared components before duplicating patterns across feature pages.

## 1. Application shell

### Sidebar
Contains the DESS identity and primary navigation. Shows active location and permission-appropriate links. On small screens, becomes a drawer.

### Top navbar
Contains page-level global controls such as current institution/context, user menu, and optional notifications. Avoid placing every page action in the global navbar.

### Breadcrumb
Shows the current location in the information hierarchy. Use links for navigable ancestors and plain text for the current page.

## 2. Page components

### Page header
- Breadcrumb (optional)
- Title (required)
- Description (optional)
- Primary action (optional)
- Secondary actions (optional)

### Section header
A small heading and optional supporting action used to organize long pages.

### Panel
A restrained white surface with a subtle border. Use for grouping related content, not as a wrapper around every element.

## 3. Data components

### Data table
Required capabilities depend on the page:
- Clear column labels.
- Consistent row spacing.
- Optional sorting.
- Optional search and filters.
- Row-level action menu where appropriate.
- Pagination for large datasets.
- Empty state.
- Loading and error state when data is fetched asynchronously.

Do not add a checkbox column unless bulk actions are actually supported.

### Pagination
Show current page, total pages or result count, previous/next controls, and page size only when useful.

### Empty state
Explain what data is absent and provide a relevant next action when the user has permission to create it. Avoid decorative illustrations that consume space.

### Filter bar
Keep frequently used filters visible. Put infrequent/advanced filters behind a clearly labeled control. Make active filters apparent and easy to clear.

## 4. Form components

### Text field
Label, optional required indicator, input, hint, and field-level error.

### Select field
Use for a bounded set of known choices. For large datasets, use searchable selection rather than a very long native select.

### Date/time fields
Use explicit labels and unambiguous date/time formats. State the timezone if the deployment context makes it necessary.

### Form section
Group related fields under a meaningful heading. Avoid one giant undifferentiated form.

### Form actions
Primary submit action, secondary cancel/back action, and destructive action separated visually.

## 5. Feedback components

### Alert
Variants: success, info, warning, danger. Each alert has a concise title or message and actionable guidance when relevant.

### Status badge
Variants: draft, pending, confirmed, conflict, inactive, cancelled. Always include text.

### Confirmation dialog
Used for irreversible or consequential actions. State the object and consequence explicitly. Destructive action must not be the default focused action.

### Validation summary
Summarizes form errors and links/focuses the corresponding fields where practical.

## 6. Scheduling components

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

## 7. Naming and implementation

Suggested JSP fragments/tag files:
```text
WEB-INF/views/components/
├── sidebar.jsp
├── navbar.jsp
├── breadcrumb.jsp
├── page-header.jsp
├── alert.jsp
├── status-badge.jsp
├── pagination.jsp
├── empty-state.jsp
└── validation-summary.jsp
```

Suggested CSS organization:
- `tokens.css`: canonical variables.
- `layout.css`: shell, sidebar, navbar, content layout.
- `components.css`: reusable controls and patterns.
- `pages.css`: narrowly scoped page-specific layout.

Do not create a shared component that hides fundamentally different behavior behind many boolean flags. Prefer a small set of composable, understandable components.
