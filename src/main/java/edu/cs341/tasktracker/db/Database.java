package edu.cs341.tasktracker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private final String jdbcUrl;
    public Database(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
    public Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(jdbcUrl);
        try (Statement st = c.createStatement()) { st.execute("PRAGMA foreign_keys=ON"); }
        return c;
    }
    public void initialize() throws SQLException {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                "password_hash TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, creator_id INTEGER NOT NULL, " +
                "assignee_id INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', " +
                "completed INTEGER NOT NULL DEFAULT 0 CHECK(completed IN (0,1)), " +
                "priority TEXT NOT NULL DEFAULT 'MEDIUM' CHECK(priority IN ('LOW','MEDIUM','HIGH')), " +
                "due_date TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (creator_id) REFERENCES users(id), FOREIGN KEY (assignee_id) REFERENCES users(id))");
            st.execute("CREATE INDEX IF NOT EXISTS ix_tasks_creator ON tasks(creator_id)");
            st.execute("CREATE INDEX IF NOT EXISTS ix_tasks_assignee ON tasks(assignee_id)");
            st.execute("CREATE INDEX IF NOT EXISTS ix_tasks_due ON tasks(due_date)");
        }
    }
}
