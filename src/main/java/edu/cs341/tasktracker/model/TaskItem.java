package edu.cs341.tasktracker.model;

public final class TaskItem {
    private final long id;
    private final long creatorId;
    private final long assigneeId;
    private final String title;
    private final String description;
    private final boolean completed;
    private final String priority;
    private final String dueDate;

    public TaskItem(long id, long creatorId, long assigneeId, String title, String description,
                    boolean completed, String priority, String dueDate) {
        this.id = id; this.creatorId = creatorId; this.assigneeId = assigneeId;
        this.title = title; this.description = description; this.completed = completed;
        this.priority = priority; this.dueDate = dueDate;
    }
    public long getId() { return id; }
    public long getCreatorId() { return creatorId; }
    public long getAssigneeId() { return assigneeId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public String getPriority() { return priority; }
    public String getDueDate() { return dueDate; }
}
