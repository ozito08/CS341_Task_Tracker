package edu.cs341.tasktracker.service;

import edu.cs341.tasktracker.model.*;
import edu.cs341.tasktracker.repository.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class TaskService {
    private final TaskRepository tasks;
    private final UserRepository users;
    public TaskService(TaskRepository tasks,UserRepository users){this.tasks=tasks;this.users=users;}
    public List<TaskItem> list(User current) throws SQLException {return tasks.visibleTo(current.getId());}
    public TaskItem create(User creator,long assignee,String title,String description,String priority,String dueDate) throws SQLException {
        validate(assignee,title,priority,dueDate);
        return tasks.insert(creator.getId(),assignee,title.trim(),clean(description),priority,cleanDate(dueDate));
    }
    public void edit(User current,long id,long assignee,String title,String description,String priority,String dueDate) throws SQLException {
        TaskItem task=mustFind(id);
        if(current.getId()!=task.getCreatorId())throw new SecurityException("Only the creator may edit a task.");
        validate(assignee,title,priority,dueDate);
        if(tasks.edit(id,current.getId(),assignee,title.trim(),clean(description),priority,cleanDate(dueDate))!=1)
            throw new SecurityException("Task was changed or is no longer available.");
    }
    public void toggle(User current,long id) throws SQLException {
        TaskItem task=mustFind(id);
        if(current.getId()!=task.getCreatorId() && current.getId()!=task.getAssigneeId())
            throw new SecurityException("You do not have access to this task.");
        if(tasks.toggleCompleted(id,current.getId())!=1)
            throw new SecurityException("Task was changed or is no longer available.");
    }
    public void delete(User current,long id) throws SQLException {
        TaskItem task=mustFind(id);
        if(current.getId()!=task.getCreatorId())throw new SecurityException("Only the creator may delete a task.");
        if(tasks.delete(id,current.getId())!=1)
            throw new SecurityException("Task was changed or is no longer available.");
    }
    private TaskItem mustFind(long id) throws SQLException {
        return tasks.findById(id).orElseThrow(()->new IllegalArgumentException("Task not found."));
    }
    private void validate(long assignee,String title,String priority,String dueDate) throws SQLException {
        if(!users.findById(assignee).isPresent())throw new IllegalArgumentException("Choose a valid assignee.");
        if(title==null || title.trim().isEmpty() || title.trim().length()>120)
            throw new IllegalArgumentException("Task title must be between 1 and 120 characters.");
        if(!("LOW".equals(priority)||"MEDIUM".equals(priority)||"HIGH".equals(priority)))
            throw new IllegalArgumentException("Invalid priority.");
        if(dueDate!=null && !dueDate.trim().isEmpty()) {
            try { LocalDate.parse(dueDate.trim()); }
            catch(DateTimeParseException e) {throw new IllegalArgumentException("Date must be YYYY-MM-DD.");}
        }
    }
    private String clean(String s){return s==null?"":s.trim();}
    private String cleanDate(String s){return s==null || s.trim().isEmpty()?null:s.trim();}
}
