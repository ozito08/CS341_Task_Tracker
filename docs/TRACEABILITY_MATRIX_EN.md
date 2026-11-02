# Requirements-to-Test Traceability Matrix

The mapped Java test cases were executed with Maven Surefire on 1 November 2026: 17 run, 0 failures, 0 errors, 0 skipped. The XML and text reports are under `target/surefire-reports/`. Manual scenarios remain pending a real Swing GUI session.

| Requirement | Main source | JUnit test case / evidence | GUI acceptance |
|---|---|---|---|
| FR-01 registration | AuthService / UserRepository | registerLoginDuplicateAndInvalidCredential, rejectedAccountsAndDatesDoNotCreateRows | AT-01 |
| FR-02 login/logout | AuthService / TaskTrackerController | activeSessionControllerIsInvalidatedOnLogoutOrFailedLogin | AT-01 |
| FR-03 creation | TaskService / TaskRepository | createAssignmentVisibilityEditToggleAndDelete | AT-02 |
| FR-04 editing | TaskService.edit / TaskRepository.edit | createAssignmentVisibilityEditToggleAndDelete, unauthorizedDirectSqlWritesCannotBypassRepository | AT-05, AT-06 |
| FR-05 deletion | TaskService.delete / TaskRepository.delete | createAssignmentVisibilityEditToggleAndDelete, creatorCanReassignExistingTaskAndAssigneeDoesNotGainDeletePermission | AT-05, AT-06 |
| FR-06 completion/reopening | TaskService.toggle | taskCanBeReopenedByAssigneeAndUnrelatedEditsFail | AT-04 |
| FR-07 assignment | TaskService.create/edit | assignmentToSelfAndToOtherUserAreBothSupported, creatorCanReassignExistingTaskAndAssigneeDoesNotGainDeletePermission | AT-02, AT-06 |
| FR-08 viewing | TaskRepository.visibleTo | createAssignmentVisibilityEditToggleAndDelete | AT-02, AT-03 |
| FR-09 SQLite/JDBC | Database, both repositories | schemaInitializationIsIdempotent, persistenceAcrossDatabaseRecreation, foreignKeyConstraintsAreEnforcedPerJdbcConnection | AT-07 |
| FR-10 priority | TaskService.validate | validationAndForeignKey, assignmentToSelfAndToOtherUserAreBothSupported | AT-08 |
| FR-11 deadline | TaskService.validate | dateAndTitleBoundaryValidation, rejectedAccountsAndDatesDoNotCreateRows | AT-08 |
| FR-12 filter/sort | MainFrame.applyFilter / JTable | No JUnit UI test; GUI manual acceptance required | AT-08 |
| NFR-01 Java 11 | Maven compiler / javac | `javac --release 11` offline pass | N/A |
| NFR-02 JUnit 5 | `src/test/java` | 17 executed; 0 failures, 0 errors, 0 skipped; see Surefire reports | N/A |
| NFR-03 MVC / OOP | model, ui, controller, service, repository, db | source / diagrams reviewed; static compile | N/A |
| NFR-04 Git process | `.github/workflows/ci.yml` | GitHub history and real 3-4 student commits pending | N/A |

## Interpretation

The Surefire reports establish the stated test execution and result, not quantitative code coverage. CI YAML is not a successful CI run. Independent Python SQLite checks do not certify Java JDBC; the separate packaged-JAR SQLite smoke test is recorded in `docs/evidence/jar-sqlite-smoke.txt`. Passing JUnit tests do not establish GUI usability. Manual GUI acceptance evidence is still required.
