# Requirements and Acceptance Criteria

**Source:** CS341 Software Engineering, Project 1, *Task Tracker – A Collaborative To-Do List Application (Java)* (5-page brief). All requirements below are derived from the brief unless marked as a design decision.

| ID | Requirement | Acceptance condition |
|---|---|---|
| FR-01 | Register user | Accept a valid unique username and password; reject duplicates. |
| FR-02 | Log in and log out | Correct credentials open a session; log out removes it. |
| FR-03 | Create task | Persist title, creator and assignee. |
| FR-04 | Edit task | Creator can update task fields. |
| FR-05 | Delete task | Creator can delete a task. |
| FR-06 | Complete task | Creator or assignee can mark complete and reopen. |
| FR-07 | Assign a specific user | Assignee is a registered user. |
| FR-08 | View relevant tasks | Only tasks created by or assigned to the active user appear. |
| FR-09 | SQLite via JDBC | Schema is created on first use; data persists after restart. |
| FR-10 | Additional feature: priority | LOW/MEDIUM/HIGH are selectable and stored. |
| FR-11 | Additional feature: deadline | Optional ISO `YYYY-MM-DD` dates are stored. |
| FR-12 | Additional feature: filter/sort | View can filter open, completed and high-priority tasks, and sort columns. |
| NFR-01 | Java 11+ | Compile with `javac --release 11`. |
| NFR-02 | JUnit 5 | Test source code and executed results documented separately. |
| NFR-03 | OOP / MVC | Model, view, controller, service, repository and persistence responsibilities distinguished. |
| NFR-04 | Git team process | Genuine commit evidence from 3–4 students, not synthetic. |
| NFR-05 | Local desktop | Runs without external database server. |

**Design decision:** Task creators may edit/delete; both creators and assignees may toggle completion. The assignment does not specify the authorization policy in this detail. **Limitation:** One local database shared through sequential sessions is not real-time collaboration across computers.
