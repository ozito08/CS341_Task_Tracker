# Test Plan (English)

## Tests and evidence

**Static/isolated checks:** Java 11 compilation; PBKDF2 self-check; Python sqlite3 schema, constraint, access-pattern, and persistence checks.

**Real integration tests (JUnit 5, require Maven and JDBC driver):** account validation, duplicate usernames, valid/invalid login, create/assign/view/edit/delete, privilege checks, reopen completion, schema idempotence, persistence, SQL injection text handling, foreign keys and input validation.

**Manual GUI tests (require desktop):** first launch with no database, create two accounts, re-login as a different user, verify visibility and authorization, task modification, filters/sort, persistence after restart, friendly invalid-input messages.

## Acceptance scenarios

| ID | Steps | Expected |
|---|---|---|
| AT-01 | Register Alice; log out; log in as Alice | Session opens and closes correctly. |
| AT-02 | Alice assigns task to Bob; log in as Bob | Task is visible to both Alice and Bob. |
| AT-03 | Log in as Carol after task created by Alice assigned to Bob | Carol does not see the task. |
| AT-04 | Bob completes the task | Status becomes completed for both users. |
| AT-05 | Bob attempts edit/delete | Operation denied; original task persists. |
| AT-06 | Alice edits/reassigns/deletes task | Changes persisted; task disappears on deletion. |
| AT-07 | Restart the app after saving | Data preserved in same `tasks.db`. |
| AT-08 | Set priority HIGH and date, use filters/sort | Table reflects chosen properties. |

## Evidence policy

Test source files are not execution results. CI YAML is not a successful CI run. Independent Python SQLite checks cannot certify the Java SQLite JDBC code. A passing Java compilation does not prove the GUI works. `target/surefire-reports` and actual GUI demonstration evidence must be captured on a machine where Maven/JDBC and a graphical session are available.
