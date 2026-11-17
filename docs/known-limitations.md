# Known limitations (MVP)

Update this file as the MVP takes shape; list it at the Day 10 demo.

- Conflicts are detected per `StudentGroup`, not per student: a student who belongs to two
  groups with overlapping sessions is not detected.
- Manual scheduling of individual teaching sessions only: no recurring timetables, bulk changes,
  substitutions or optimisation.
- Programs and versions exist, but there is no curriculum editor, credit model or prerequisite engine.
- Results have a status and a score only: no averages, weights, retakes or ranking.
- One institution per database and per local installation: no tenant switching.
- Authentication has two roles only: admin and professor.
- The web UI is a stretch goal: the REST API is the acceptance surface.
