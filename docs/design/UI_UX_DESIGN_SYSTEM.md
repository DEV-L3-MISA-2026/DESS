# DESS UI/UX Design System

**Version:** 1.0  
**Status:** Initial project baseline  
**Product:** Department Exam and Schedule System

## 1. Product experience

DESS is an academic administration system used to manage institutional structure, people, academic data, teaching schedules, examinations, and results.

The interface must help staff perform administrative work accurately and efficiently. It is not a marketing website and should not resemble a generic analytics SaaS template.

### Design principles
1. **Clarity over decoration** — prioritize readable information and explicit actions.
2. **Consistency over novelty** — repeated tasks should look and behave the same way.
3. **Accuracy over speed tricks** — destructive and scheduling actions require clear context and confirmation.
4. **Progressive disclosure** — show essential information first; reveal advanced details when needed.
5. **Traceability** — make statuses, ownership, dates, and validation outcomes understandable.
6. **Accessible by default** — keyboard navigation, semantic structure, labels, focus, and contrast are required.
7. **Responsive, desktop-first** — administrative work is primarily desktop-oriented, but core tasks must remain usable on tablets and phones.

## 2. Visual direction

Use a restrained institutional visual language:
- Deep navy for structure and primary navigation.
- Muted teal for primary actions and selected states.
- Warm, very light neutral canvas.
- White surfaces and subtle borders.
- Clear typography and compact but comfortable spacing.
- Minimal motion; transitions should never delay work.

Avoid:
- Large gradient backgrounds.
- Glassmorphism and translucent panels.
- Excessive rounded cards.
- Large decorative illustrations on data-heavy pages.
- Emoji as interface icons.
- Unnecessary animation or animated counters.
- Color-only status indicators.

## 3. Application shell

### Desktop
- Fixed or sticky left sidebar, approximately 250px wide.
- Top bar approximately 64px high.
- Main content area uses a light neutral canvas.
- Main content has a consistent horizontal gutter, generally 24–32px.
- Content should have a readable maximum width where full-width tables or calendars are not needed.

### Navigation
Primary sections:
- Overview
- Institution
- People
- Academic
- Scheduling
- Examinations
- Results
- Security

Navigation must be permission-aware. The backend must still authorize every request independently.

### Page header
Every major page should use a consistent header containing:
- Breadcrumbs where hierarchy is meaningful.
- Page title.
- Short explanatory sentence only when it adds context.
- Primary action aligned consistently.
- Optional secondary actions or filters.

### Responsive behavior
- Sidebar becomes a collapsible navigation drawer or compact navigation on narrow screens.
- Tables may scroll horizontally, but do not force the entire page to overflow.
- Forms become single-column on narrow screens.
- Timetables must provide a list/day alternative on mobile.
- Keep primary actions visible and reachable.

## 4. Design tokens

The canonical values are in `DESIGN_TOKENS.md`. Do not redefine them per page.

## 5. Typography

Use Inter when locally available or loaded through the project's approved asset strategy. Provide a system sans-serif fallback.

Typography should be compact and readable:
- Page title: 26px, semibold.
- Section title: 19px, semibold.
- Component/card title: 15px, semibold.
- Body: 14px, regular.
- Table content: 13px, regular.
- Form label: 12px, medium.
- Metadata: 11px, regular.

Use sentence case. Avoid all-caps labels except for short conventional abbreviations.

## 6. Core interaction patterns

### Forms
- Labels appear above controls.
- Required fields are explicitly marked and explained.
- Group related fields under meaningful section headings.
- Show validation close to the relevant field and summarize errors when a form has multiple errors.
- Preserve safe user-entered values after server-side validation failure.
- Distinguish Save, Save and continue, Cancel, and destructive actions.

### Tables
- Use clear column headings and consistent alignment.
- Keep row actions predictable and avoid overcrowding each row.
- Provide search/filter controls only when useful for the dataset.
- Include pagination for large datasets.
- Provide a useful empty state and a clear way to create the first record when authorized.
- Make sorting state accessible and visible.

### Status
Use text labels with status colors:
- Draft
- Pending
- Confirmed
- Conflict
- Inactive
- Cancelled
Status must remain understandable without color.

### Feedback
- Success messages state what happened.
- Error messages explain what failed and, when possible, how to fix it.
- Destructive actions require confirmation and identify the affected object.
- Avoid vague messages such as “Something went wrong” when a specific explanation is available.

### Dialogs
Use dialogs for short decisions or confirmations, not for complex multi-step forms. A dialog must have a clear title, explicit actions, keyboard behavior, and a close/cancel path.

## 7. Scheduling experience

Scheduling is a central DESS workflow.

Provide perspectives by:
- Student group.
- Teacher.
- Room.

A scheduling view should make the selected perspective and filters obvious. Each event should show the most relevant details without requiring a click for basic identification.

When creating or editing a session, collect:
- Course offering or assessment.
- Group or candidate scope, as appropriate.
- Teacher/supervisor where applicable.
- Room.
- Date.
- Start and end time.

Before confirmation, validate:
- Room availability.
- Teacher availability.
- Group availability.
- Valid time range and applicable academic constraints.

When a conflict occurs:
- Do not silently discard the attempted action.
- Identify the conflicting resource.
- Show the conflicting session's date/time and relevant label when authorized.
- Offer a clear correction path.
- Do not present a conflicted session as confirmed.

The mobile alternative should be a chronological list grouped by day, not a compressed unreadable weekly grid.

## 8. JSP implementation conventions

- Store views under `src/main/webapp/WEB-INF/views/`.
- Use JSP includes or tag files for shared fragments.
- Use JSTL and Expression Language; no scriptlets.
- Keep controllers thin and delegate to application services.
- Keep business rules and authorization checks on the server.
- Encode output according to its context.
- Use Post/Redirect/Get for successful form submissions where appropriate.
- Include CSRF protection for state-changing requests.
- Do not put secrets or authorization logic in client-side JavaScript.

Suggested view layout:

```text
WEB-INF/views/
├── layouts/
│   └── main.jsp
├── components/
│   ├── sidebar.jsp
│   ├── navbar.jsp
│   ├── breadcrumb.jsp
│   ├── alert.jsp
│   └── pagination.jsp
├── dashboard/
├── institution/
├── people/
├── academic/
├── scheduling/
├── assessment/
└── results/
```

## 9. Definition of done for a page

A page is not complete until:
- It follows the design tokens and shared components.
- It has a clear title and navigation context.
- Its main task and primary action are obvious.
- Forms have labels, validation, and useful error messages.
- Empty, loading (if applicable), and error states are handled.
- It works at desktop and narrow viewport widths.
- Keyboard focus and interaction are usable.
- User-controlled content is safely encoded.
- Server-side authorization and validation are implemented.
- Relevant tests/build checks pass.
