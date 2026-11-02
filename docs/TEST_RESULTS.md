# Verification Results — CS341 Task Tracker

Verification date: 1 November 2026  
Environment: Windows 11, JDK 22.0.1, Maven 3.9.9; compiler configured with `--release 11`.

| Verification | Status | Evidence |
|---|---|---|
| Maven clean build and JUnit 5/JDBC tests | PASS — 17 run, 0 failures, 0 errors, 0 skipped | `docs/evidence/maven-clean-verify.log`; `target/surefire-reports/` |
| Executable shaded JAR contains SQLite JDBC and native resources | PASS | `docs/evidence/jar-sqlite-smoke.txt`; JAR contains `org/sqlite/JDBC.class`, `META-INF/services/java.sql.Driver`, and Windows native libraries |
| SQLite JDBC create/insert/read from packaged JAR | PASS | `docs/evidence/jar-sqlite-smoke.txt` |
| Independent Python `sqlite3` checks | PASS — 10/10 | `docs/SQLITE_OFFLINE_RESULTS.json`; supplementary to JDBC/JUnit |
| Seven genuine PlantUML diagrams rendered to SVG and PNG | PASS — 7/7 | `docs/evidence/plantuml-svg.log`, `docs/evidence/plantuml-png.log`, `docs/diagrams/` |
| XeLaTeX rebuild of all six PDFs, two passes each; extracted text and page counts checked | PASS — 6 PDFs, 36 total pages | `docs/evidence/latex/`; `docs/evidence/PDF_PREFLIGHT_EN.json` |
| Previously verified ZIP CRC/integrity and extracted Maven build | PASS (prior local build) — 144 entries checked in original archive | `docs/evidence/release-zip-verification.txt` (previous Windows build workspace) |
| Manual Swing GUI acceptance scenarios | NOT RUN | A real interactive GUI session and screenshots are still required |
| GitHub Actions run | NOT RUN | No repository was published or pushed |
| 3–4 real team-member contributions and presentation | NOT VERIFIED | Must be supplied by the actual team |

The first `mvn -B clean verify` attempt passed all tests but failed once while assembling the JAR. A direct Maven package retry and the subsequent clean verification both succeeded; the successful clean run is the result reported above. The initial failed output is retained as `docs/evidence/maven-package-debug.log` and is not hidden.

The JUnit results establish service/repository/database behavior for the test cases listed in the Surefire reports. They do not replace manual GUI testing or prove actual course-team participation. Do not claim a completed GitHub CI run or class demonstration from local files.
