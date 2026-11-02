# Architecture and Design Decisions

## Runtime model

Single-machine Swing desktop program. `App` initializes SQLite and assembles dependencies. `MainFrame` handles user interaction. `TaskTrackerController` owns the active session. `AuthService` and `TaskService` enforce business rules. `UserRepository` and `TaskRepository` execute parameterized SQL against `Database` connection creation.

## Data model

`users(id, username, password_hash, created_at)` and `tasks(id, creator_id, assignee_id, title, description, completed, priority, due_date, created_at)` are connected by two foreign keys. The schema is also available in `sql/schema.sql`.

## Design notes

- `COLLATE NOCASE UNIQUE` gives case-insensitive usernames.
- Password hashes use PBKDF2-HMAC-SHA256 with a random 16-byte salt. The implementation stores iteration count, salt and digest, not the cleartext password.
- Prepared statements defend query text from being treated as SQL syntax.
- Authorization is checked in the service and again in conditional SQL mutations.
- Reopening an existing local database preserves tasks and users.
- UI filter and table sorting are client-side only. There is no server or online synchronization.

## Known limits

There is no multi-device synchronization, administrator workflow, password reset, user deactivation, encrypted local DB, or automated backup. Local file permissions protect `tasks.db`; users with OS-level access to the file may inspect its contents. SQLite single-file operation was chosen because the coursework explicitly requests it.
