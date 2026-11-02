# User and Installation Guide

## Prerequisites

Java JDK 11 or higher, Maven 3.8 or higher, and a machine with a graphical desktop. The first Maven build needs access to dependency repositories. There is no separate database server. The verified build used JDK 22.0.1 and Maven 3.9.9 while compiling application code for Java 11.

## Build and launch

From the project root run:

```
mvn -B clean verify
java -jar target/task-tracker-1.0.0.jar
```

On Windows, the build and test can also be run with `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\verify_windows.ps1`.

A single local database file `tasks.db` is created automatically. Back up the file manually if needed; the app does not include automatic backups.

## Main operations

1. Create a username (3–30 ASCII letters, digits, or underscores) and a password (at least 8 characters).
2. Log in. Use **New task** to enter a title, choose an assignee, optionally set priority/date and save.
3. Select a task to **Edit task**, **Toggle complete**, or **Delete**. Editing and deletion require being the creator; completion toggling also works for the assignee.
4. Use **Filter** or click column headings to filter/sort the visible list.
5. Log out, then log in under another account to simulate collaborative task exchange on the same computer.

## Troubleshooting

- `mvn: command not found`: install Maven and ensure `mvn` is on PATH.
- `No suitable driver ... jdbc:sqlite`: launch the Maven-shaded runnable JAR after a successful build, not just compiled `.class` files.
- No window appears: run in a normal graphical desktop (not a headless session).
- Permission denied: only creators may edit/delete; assignees can toggle completion.
- Date validation error: use ISO `YYYY-MM-DD` and a real calendar date.
