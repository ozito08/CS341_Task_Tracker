# Final Audit — CS341 Task Tracker

Audit date: 1 November 2026  
Status: **PARTIALLY VERIFIED**

## Initial inventory and findings

- The initial project archive contained source, tests, reports and scripts but no root `pom.xml`; its README, Windows run script and CI workflow all expected Maven.
- The extracted source package is not a Git repository. No existing history or user changes were available to preserve or commit.
- Maven initially failed because the inherited `JAVA_HOME` pointed to a nonexistent JDK 21 directory. The installed JDK is `C:\Program Files\Java\jdk-22`; setting that value for the verification process enabled Maven.
- The existing Python SQLite verifier leaked a reopened SQLite connection on Windows. Python 3.14 raised `PermissionError` while cleaning its temporary database; the connection is now explicitly closed.
- The supplied PDF assignment was extracted and reviewed. It specifies a four-week team project, 3–4 real members, member contributions/commits, GitHub submission, a demonstration and the 25/30/20/15/10 rubric weights. No team identities, commits, remote CI run or presentation evidence were invented.

## Implemented and executed

- Added the root Maven build with pinned plugin/dependency versions, UTF-8, Java 11 release targeting, JUnit 5/Surefire, Xerial SQLite JDBC and an executable shaded JAR.
- `mvn -B clean verify`: **PASS**, exit code 0; 17 tests run, 0 failures, 0 errors, 0 skipped. The XML/TXT reports are under `target/surefire-reports/`.
- The first clean verification passed all tests but failed once during JAR assembly. A diagnostic package retry and the next clean verification succeeded; both failed and successful logs remain under `docs/evidence/`.
- A separate temporary Java smoke program loaded the packaged JAR's SQLite driver and completed a database create/insert/read round trip: **PASS**.
- `python scripts/sqlite_offline_check.py`: **PASS**, 10/10 independent SQLite checks. It is not JDBC evidence.
- PlantUML 1.2025.10 rendered all seven canonical sources as both SVG and PNG: **PASS**, 7/7.
- The six XeLaTeX PDFs were rebuilt after the diagram render. Their build logs and text/page preflight are recorded under `docs/evidence/latex/`.

## Acceptance gate matrix

| Gate | Status | Evidence / remaining work |
|---|---|---|
| G1 Maven build configuration and dependency resolution | PASS | `pom.xml`; successful Maven logs |
| G2 Real Maven/JUnit execution | PASS | 17/17; `target/surefire-reports/` |
| G3 Packaged JAR SQLite access | PASS | `docs/evidence/jar-sqlite-smoke.txt` |
| G4 Required application behavior | PARTIAL | Service/repository behaviors covered by JUnit; filter/sort and full workflows still need manual GUI acceptance |
| G5 Genuine PlantUML render | PASS | Seven SVG and seven PNG files in `docs/diagrams/` |
| G6 Six English PDFs rebuilt and preflighted | PASS | Two XeLaTeX passes each; extracted text/page count verified; source PDFs checked with extracted text and page counts; `docs/evidence/latex/` |
| G7 Documentation/evidence consistency | PARTIAL | Updated local claims; remote/team evidence remains pending |
| G8 Previous complete archive extraction and independent build | PASS (prior Windows verification) | Extracted Maven verification passed; final document-only repack still requires a local Maven run |
| G9 Real team, GitHub and class presentation | PENDING | Requires actual students and explicit network/submission actions |

## Human and remote steps

1. Perform AT-01 through AT-08 in a real interactive Swing session and record genuine results/screenshots.
2. Add the actual 3–4 team members' contributions and authentic individual commit history. The supplied extracted directory has no `.git`; do not fabricate prior history.
3. With team approval, publish to the required GitHub repository, run its workflow, and record the real CI result.
4. Deliver the live class presentation with all actual team members participating.

## Evidence paths

- Maven log: `docs/evidence/maven-clean-verify.log`
- Initial packaging failure and retry: `docs/evidence/maven-clean-verify.log`, `docs/evidence/maven-package-debug.log`
- Surefire XML/TXT: `target/surefire-reports/`
- Packaged JAR SQLite smoke: `docs/evidence/jar-sqlite-smoke.txt`
- Python SQLite results: `docs/SQLITE_OFFLINE_RESULTS.json`
- PlantUML logs and generated images: `docs/evidence/plantuml-*.log`, `docs/diagrams/`
- LaTeX logs, extracted text and preflight: `docs/evidence/latex/`
- Final distribution: `dist/CS341_Task_Tracker_FINAL.zip`; SHA-256 sidecar: `dist/SHA256_MANIFEST.txt`
- Previous archive integrity and extracted-build logs are retained in `docs/evidence/`. The renamed package is checked for ZIP integrity and file inventory; no new Windows Maven or GUI claim is made.
