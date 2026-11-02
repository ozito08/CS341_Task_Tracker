package edu.cs341.tasktracker.repository;

import edu.cs341.tasktracker.db.Database;
import edu.cs341.tasktracker.model.TaskItem;
import java.sql.*;
import java.util.*;

public final class TaskRepository {
    private final Database database;
    public TaskRepository(Database database) { this.database=database; }
    private TaskItem map(ResultSet r) throws SQLException {
        return new TaskItem(r.getLong("id"),r.getLong("creator_id"),r.getLong("assignee_id"),
            r.getString("title"),r.getString("description"),r.getInt("completed")==1,
            r.getString("priority"),r.getString("due_date"));
    }
    public List<TaskItem> visibleTo(long userId) throws SQLException {
        List<TaskItem> result=new ArrayList<>();
        try(Connection c=database.connect(); PreparedStatement ps=c.prepareStatement(
                "SELECT * FROM tasks WHERE creator_id=? OR assignee_id=? ORDER BY completed ASC, due_date IS NULL, due_date, id DESC")) {
            ps.setLong(1,userId); ps.setLong(2,userId);
            try(ResultSet rs=ps.executeQuery()){while(rs.next())result.add(map(rs));}
        }
        return result;
    }
    public Optional<TaskItem> findById(long id) throws SQLException {
        try(Connection c=database.connect(); PreparedStatement ps=c.prepareStatement("SELECT * FROM tasks WHERE id=?")) {
            ps.setLong(1,id); try(ResultSet rs=ps.executeQuery()) {return rs.next()?Optional.of(map(rs)):Optional.empty();}
        }
    }
    public TaskItem insert(long creator,long assignee,String title,String description,String priority,String dueDate) throws SQLException {
        try(Connection c=database.connect(); PreparedStatement ps=c.prepareStatement(
                "INSERT INTO tasks(creator_id,assignee_id,title,description,priority,due_date) VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1,creator);ps.setLong(2,assignee);ps.setString(3,title);ps.setString(4,description);
            ps.setString(5,priority);ps.setString(6,dueDate);ps.executeUpdate();
            try(Statement st=c.createStatement(); ResultSet rs=st.executeQuery("SELECT last_insert_rowid()")) {
                if(rs.next())return new TaskItem(rs.getLong(1),creator,assignee,title,description,false,priority,dueDate);
            }
        }
        throw new SQLException("Task ID was not returned");
    }
    public int edit(long id,long creator,long assignee,String title,String description,String priority,String dueDate) throws SQLException {
        try(Connection c=database.connect(); PreparedStatement ps=c.prepareStatement(
                "UPDATE tasks SET assignee_id=?,title=?,description=?,priority=?,due_date=? WHERE id=? AND creator_id=?")) {
            ps.setLong(1,assignee);ps.setString(2,title);ps.setString(3,description);
            ps.setString(4,priority);ps.setString(5,dueDate);ps.setLong(6,id);ps.setLong(7,creator);return ps.executeUpdate();
        }
    }
    public int toggleCompleted(long id,long userId) throws SQLException {
        try(Connection c=database.connect(); PreparedStatement ps=c.prepareStatement(
                "UPDATE tasks SET completed=1-completed WHERE id=? AND (creator_id=? OR assignee_id=?)")) {
            ps.setLong(1,id);ps.setLong(2,userId);ps.setLong(3,userId);return ps.executeUpdate();
        }
    }
    public int delete(long id,long creatorId) throws SQLException {
        try(Connection c=database.connect(); PreparedStatement ps=c.prepareStatement(
                "DELETE FROM tasks WHERE id=? AND creator_id=?")) {
            ps.setLong(1,id);ps.setLong(2,creatorId);return ps.executeUpdate();
        }
    }
}
