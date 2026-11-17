# Glossary (ubiquitous language)

Single source of vocabulary for code, API, database and conversations.
If a term is not here, agree on it before using it in code.

| Concept | Meaning |
|---|---|
| `OrganizationalUnit` | A node in the institution tree: campus, faculty, department, school or academic level. |
| `StudentGroup` | A cohort taught and examined together. It has an owning organizational unit; it is **not** an organizational unit. |
| `AcademicProgram` | A named course of study, such as BSc Computer Science. |
| `ProgramVersion` | A dated or named version of a program's rules. A student enrolls against a version. |
| `ProgramEnrollment` | A student's enrollment in an `AcademicProgram` and `ProgramVersion`. |
| `Course` | A subject in the catalogue, such as Mathematics. |
| `CourseOffering` | A delivery of a course to one or more groups in an academic period, with assigned teachers. |
| `TeachingSession` | One dated, time-bounded class meeting for a course offering. Owned by `academic`. |
| `Assessment` | What is evaluated: exam or test, maximum score and basic settings. |
| `ExamSession` | When and where an assessment takes place, with candidates and supervisors. Owned by `assessment`. |
| `Booking` | A reservation of a bookable resource for a time range. Owned by `scheduling`. |

## Naming rules

- Do **not** add `CLASS_GROUP` as an organizational-unit type. `StudentGroup` is the only teaching cohort.
- Do **not** add `Map<String, Object>` custom attributes to units (post-MVP, and typed if ever added).
- Result statuses: `SCORED`, `ABSENT`, `EXCUSED`.
