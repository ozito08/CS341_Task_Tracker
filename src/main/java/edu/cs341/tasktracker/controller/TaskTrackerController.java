package edu.cs341.tasktracker.controller;

import edu.cs341.tasktracker.model.TaskItem;
import edu.cs341.tasktracker.model.User;
import edu.cs341.tasktracker.repository.UserRepository;
import edu.cs341.tasktracker.service.AuthService;
import edu.cs341.tasktracker.service.TaskService;
import java.sql.SQLException;
import java.util.List;

/** Application controller: owns session and coordinates view operations. */
public final class TaskTrackerController {
    private final AuthService authentication;
    private final TaskService tasks;
    private final UserRepository users;
    private User signedIn;

    public TaskTrackerController(AuthService authentication, TaskService tasks, UserRepository users) {
        this.authentication=authentication; this.tasks=tasks; this.users=users;
    }
    public User register(String name,char[] password) throws SQLException {
        signedIn=null;
        signedIn=authentication.register(name,password);
        return signedIn;
    }
    public User login(String name,char[] password) throws SQLException {
        signedIn=null;
        signedIn=authentication.login(name,password);
        return signedIn;
    }
    public void logout() { signedIn=null; }
    public User currentUser() { return requireSession(); }
    public List<User> allUsers() throws SQLException { requireSession(); return users.all(); }
    public List<TaskItem> tasks() throws SQLException { return tasks.list(requireSession()); }
    public TaskItem create(long assignee,String title,String description,String priority,String dueDate) throws SQLException {
        return tasks.create(requireSession(),assignee,title,description,priority,dueDate);
    }
    public void edit(long id,long assignee,String title,String description,String priority,String dueDate) throws SQLException {
        tasks.edit(requireSession(),id,assignee,title,description,priority,dueDate);
    }
    public void toggle(long id) throws SQLException { tasks.toggle(requireSession(),id); }
    public void delete(long id) throws SQLException { tasks.delete(requireSession(),id); }
    private User requireSession() {
        if(signedIn==null)throw new SecurityException("Please log in first.");
        return signedIn;
    }
}
