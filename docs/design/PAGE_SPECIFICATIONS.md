# DESS Page Specifications

These are functional UI specifications for the first release. They define expected user-facing behavior, not database schemas or final backend contracts.

## 1. Overview dashboard

### Purpose
Give staff a concise view of the current academic context and actions requiring attention.

### Content
- Current academic year/period.
- Upcoming teaching sessions or examinations.
- Pending tasks relevant to the current user's permissions.
- Scheduling conflicts requiring attention.
- Useful counts only when they help a decision.

### Behavior
- Each summary links to the corresponding filtered list.
- Do not show fabricated metrics or placeholder charts in production.
- Empty states explain when no upcoming events or pending actions exist.

### Implementation status
The page shows the counts the module services can provide and the next six
active bookings, earliest first, with UTC times. The current period, pending
tasks and scheduling conflicts remain roadmap items: the academic module has no
notion of a current period, and no task or conflict query exists. Do not fake
those blocks.

## 2. Institution structure

### Purpose
Browse and maintain the institution's organizational hierarchy.

### Main view
- Tree or hierarchical list of organizational units.
- Unit name and type.
- Parent relationship.
- Active/inactive state.
- Create, edit, view, and deactivate actions according to permissions.

### XML configuration
- Provide an import action.
- Show validation results before applying changes.
- Distinguish XML/schema errors from semantic/business validation errors.
- Show the affected units and explain invalid parent-child relationships.
- Require confirmation before applying a valid import.

## 3. People

### Purpose
Find and manage students, professors, and staff.

### Main view
- Search by name or institutional identifier.
- Filters appropriate to the selected person type.
- Table columns should include identifier, name, relevant academic/organizational context, and status.
- Profile/detail page for a selected person.

### Behavior
- Make the distinction between a Person record and a login UserAccount clear.
- Do not imply that every person has a login account.
- Account management actions belong to authorized security workflows.

## 4. Academic

### Purpose
Manage academic periods, groups, courses, programs, program versions, and enrollments.

### Main pages
- Academic periods.
- Student groups.
- Courses.
- Course offerings.
- Academic programs and program versions.
- Program enrollments.

### Behavior
- Clearly distinguish a Course (catalog concept) from a CourseOffering (a course delivered in a particular academic context).
- Show relationships and status without requiring users to infer them from IDs.
- Avoid presenting future roadmap features as available in the MVP.

## 5. Scheduling — teaching timetable

### Purpose
Schedule regular teaching sessions and detect resource conflicts.

### Main view
- Weekly or daily calendar on desktop.
- Group, teacher, and room perspectives.
- Filters for academic period, course, group, teacher, and room as applicable.
- List alternative on mobile.

### Create/edit session
Fields:
- Course offering.
- Group.
- Teacher.
- Room.
- Date.
- Start time.
- End time.

### Validation
Check room, teacher, and group overlap. Explain each conflict clearly. A failed validation must not create a confirmed booking.

### Implementation status
The page is currently a room register with a read-only booking list. Rooms can
be created. Each booking shows its owner, the reserved resources (room codes),
the UTC time range and the status.

The weekly or daily calendar, the group, teacher and room perspectives, the
filters, the create/edit session form and the on-page conflict explanation are
roadmap items. They need course offerings, student groups, teaching sessions and
a conflict query that the backend does not have yet. Until then the page must
not imply that a timetable or a booking action exists. Exam sessions already
create bookings through the scheduling service.

## 6. Examinations

### Purpose
Create assessments, manage exam sessions, and identify eligible candidates.

### Main pages
- Assessment list.
- Assessment detail.
- Exam session list/calendar.
- Candidate/eligibility view.

### Exam session
Capture the assessment, date, start/end time, room, supervisors, and candidate scope as required by the domain model.

### Behavior
- Reuse the same scheduling concepts and conflict feedback used by teaching timetable.
- Make candidate eligibility visible before finalizing the session.
- Do not silently include ineligible candidates.

### Implementation status
Exam sessions persist and `/app/exam-sessions` can schedule, search, replace
invigilators and cancel them. `/app/examinations` is a read-only session list
with UTC times.

The assessment list and detail pages, the candidate/eligibility view and a
session calendar are roadmap items. Assessment definitions and eligibility
exist in the application layer but have no page yet.

## 7. Results

### Purpose
Record, validate, and consult assessment results.

### Main view
- Assessment and session context.
- Candidate list.
- Mark/result entry where permitted.
- Draft/submitted/validated state as supported by backend workflow.

### Behavior
- Clearly identify the assessment, course, period, and candidate.
- Validate input on the server.
- Make the difference between saving a draft and validating/publishing results explicit.
- Do not introduce averages, weighting, ranking, or retake workflows unless they are explicitly included in the release scope.

### Implementation status
`/app/results` lists each exam session with the number of results recorded for
its assessment, using `ResultService`. The page never shows a mark that was not
stored.

Result entry, correction and a candidate-level result list have no page yet.
The model stores final results only: there is no draft, submitted or validated
state to display. Those states, averages, weighting and ranking stay out of the
release until they are explicitly added.

## 8. Security

### Purpose
Manage login accounts, roles, and permissions.

### Behavior
- Separate person identity from login account.
- Show role assignments and account status.
- Never display password values or password hashes.
- Enforce all authorization on the server.
- Navigation visibility is only a usability aid, not a security boundary.
