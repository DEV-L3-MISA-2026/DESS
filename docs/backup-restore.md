# Backup and restore

Backups are PostgreSQL custom-format dumps stored in `backups/` (ignored by Git).

```bash
./ops/scripts/backup.sh                          # creates backups/dess-YYYYMMDD-HHMMSS.dump
./ops/scripts/restore.sh backups/<file>.dump     # replaces the current database content
```

- Stop the application before restoring.
- Rehearse a full backup and restore on Day 9 from a clean checkout.
- Keep copies of important backups outside the machine that runs the database.
