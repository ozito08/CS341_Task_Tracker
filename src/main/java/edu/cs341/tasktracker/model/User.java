package edu.cs341.tasktracker.model;

public final class User {
    private final long id;
    private final String username;
    private final String passwordHash;

    public User(long id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    @Override public String toString() { return username; }
}
