package edu.cs341.tasktracker.repository;

import edu.cs341.tasktracker.db.Database;
import edu.cs341.tasktracker.model.User;
import java.sql.*;
import java.util.*;

public final class UserRepository {
    private final Database database;
    public UserRepository(Database database) { this.database = database; }
    private User from(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"));
    }
    public Optional<User> findByUsername(String username) throws SQLException {
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next()? Optional.of(from(rs)): Optional.empty(); }
        }
    }
    public Optional<User> findById(long id) throws SQLException {
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM users WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next()? Optional.of(from(rs)): Optional.empty(); }
        }
    }
    public List<User> all() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM users ORDER BY username"); ResultSet rs = ps.executeQuery()) {
            while(rs.next()) list.add(from(rs));
        }
        return list;
    }
    public User insert(String username, String hash) throws SQLException {
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(username,password_hash) VALUES(?,?)")) {
            ps.setString(1,username); ps.setString(2,hash); ps.executeUpdate();
            try (Statement st=c.createStatement(); ResultSet rs=st.executeQuery("SELECT last_insert_rowid()")) {
                if(rs.next()) return new User(rs.getLong(1),username,hash);
            }
        }
        throw new SQLException("User ID was not returned");
    }
}
