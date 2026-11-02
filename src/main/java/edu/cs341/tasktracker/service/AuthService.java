package edu.cs341.tasktracker.service;

import edu.cs341.tasktracker.model.User;
import edu.cs341.tasktracker.repository.UserRepository;
import java.sql.SQLException;
import java.util.Optional;

public final class AuthService {
    private final UserRepository users;
    private final PasswordHasher hasher;
    public AuthService(UserRepository users,PasswordHasher hasher){this.users=users;this.hasher=hasher;}
    public User register(String name,char[] password) throws SQLException {
        String username=name==null?"":name.trim();
        if(!username.matches("[A-Za-z0-9_]{3,30}"))throw new IllegalArgumentException("Username: 3-30 letters, numbers or underscores.");
        if(password==null || password.length<8)throw new IllegalArgumentException("Password must contain at least 8 characters.");
        if(users.findByUsername(username).isPresent())throw new IllegalArgumentException("Username already exists.");
        try {return users.insert(username,hasher.hash(password));}
        catch(SQLException e) {if(e.getMessage()!=null && e.getMessage().contains("UNIQUE"))
            throw new IllegalArgumentException("Username already exists.");throw e;}
    }
    public User login(String name,char[] password) throws SQLException {
        if(name==null || password==null)throw new IllegalArgumentException("Invalid username or password.");
        Optional<User> user=users.findByUsername(name.trim());
        if(!user.isPresent() || !hasher.verify(password,user.get().getPasswordHash()))
            throw new IllegalArgumentException("Invalid username or password.");
        return user.get();
    }
}
