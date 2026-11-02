# CS341 Task Tracker

## Project summary

A standalone **Java 11-compatible / Swing** collaborative to-do application. Multiple accounts use the **same local SQLite file** in separate login sessions. This is **not** network synchronization, a hosted service, or simultaneous multi-device collaboration.

Required features: account creation, login/logout, task creation, assignment, editing, deletion, completion/reopening, viewing created/assigned tasks and SQLite persistence through JDBC. Additional features: due dates, priority levels and task filtering/sorting.

## Quick start

Prerequisites: Java JDK 11+ and Maven 3.8+; access to Maven Central during the first dependency download. The verified build used JDK 22.0.1 and Maven 3.9.9, with application bytecode targeted to Java 11.

```bash
mvn -B clean verify
java -jar target/task-tracker-1.0.0.jar
```

Windows:

```powershell
mvn -B clean verify
java -jar target\task-tracker-1.0.0.jar
```

For a Windows-native verification (including a packaged-JAR SQLite smoke test), run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\verify_windows.ps1
```

For the full local checks (PlantUML renders and six XeLaTeX PDFs), run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\verify_all_windows.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build_release_windows.ps1
```

The full verification script downloads PlantUML 1.2025.10 from Maven Central to the current user's local application-data tools folder when no CLI or `PLANTUML_JAR` is configured.

The application creates `tasks.db` automatically in the working directory. For a custom path:

```bash
java -Dtasktracker.db=/path/to/tasks.db -jar target/task-tracker-1.0.0.jar
```

## Code organization

- `src/main/java/edu/cs341/tasktracker/` — model, view (`MainFrame`), controller, services, repositories and SQLite setup.
- `src/test/java/` — JUnit 5 integration and password-hashing tests.
- `sql/schema.sql` — reference SQL schema.
- `.github/workflows/ci.yml` — CI configuration **not evidence of an executed GitHub run**.
- `docs/latex/*.tex` — plain English LaTeX sources (XeLaTeX).
- `docs/plantuml/*.puml` — canonical editable PlantUML diagrams.
- `docs/diagrams/` — black-and-white PNG and SVG renders generated from the PlantUML sources.
- `docs/*.pdf` — generated plain English documents and Beamer slides.

## Verification and reproducible builds

```bash
bash scripts/verify_project.sh
bash scripts/render_plantuml.sh
bash scripts/render_latex.sh
```

`verify_project.sh` **fails with exit code 2 if Maven is missing**. Its Java compile/password smoke and Python SQLite engine checks are useful independent checks, but **not substitutes for Java/JDBC/JUnit integration testing**. Run `bash scripts/verify_project.sh --offline-only` only if you intentionally want those partial checks.

The PlantUML script requires an installed `plantuml` command or an official PlantUML JAR (`PLANTUML_JAR=C:\path\to\plantuml.jar`). It renders the seven sources under `docs/plantuml/` into `docs/diagrams/`; rebuild LaTeX PDFs after changing a diagram.

The current local evidence is in `docs/evidence/`: `mvn -B clean verify` passed all 17 JUnit tests, a separate smoke program confirmed SQLite JDBC works from the shaded executable JAR, all seven PlantUML diagrams were rendered to PNG and SVG, and the independent Python SQLite check passed 10/10 checks. These results do not include a manual Swing GUI acceptance run or a remote GitHub Actions run. Read `docs/FINAL_AUDIT_EN.md` before treating the project as ready for submission.

The verified release archive is `dist/CS341_Task_Tracker_FINAL.zip`; its SHA-256 is recorded in `dist/SHA256_MANIFEST.txt`.

## Authorization model

- Task creator: create, view, edit, delete, toggle completion, reassign.
- Task assignee: view and toggle completion (not edit/delete).
- Unrelated user: no task visibility or mutation access.
- Usernames are unique irrespective of case; passwords are stored with randomly salted PBKDF2 hashes; SQL statements use prepared parameters.

## Submission caveat

The CS341 brief requires a **3–4 student team**, actual team contribution history, a GitHub repository, JUnit execution evidence, and a live presentation. These cannot be manufactured. See `docs/Gap_Audit_EN.pdf`, `docs/Test_Report_EN.pdf`, and `docs/SUBMISSION_CHECKLIST.md` for remaining human/environment-dependent steps.
