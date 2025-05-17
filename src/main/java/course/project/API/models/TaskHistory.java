package course.project.API.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_history")
public class TaskHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String actionType; // "CREATE", "UPDATE", "DELETE"
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "task_id", nullable = true)
    private Long taskId;
    
    @Column(nullable = false)
    private Long boardId;
    
    @Column(columnDefinition = "TEXT")
    private String oldTaskJson;
    
    @Column(columnDefinition = "TEXT")
    private String newTaskJson;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String taskSnapshot;
    
    public TaskHistory() {
    }
    
    public TaskHistory(String actionType, User user, Long taskId, Long boardId, String oldTaskJson, String newTaskJson, String taskSnapshot) {
        this.actionType = actionType;
        this.timestamp = LocalDateTime.now();
        this.user = user;
        this.taskId = taskId;
        this.boardId = boardId;
        this.oldTaskJson = oldTaskJson;
        this.newTaskJson = newTaskJson;
        this.taskSnapshot = taskSnapshot;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public Long getBoardId() {
        return boardId;
    }
    
    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }
    
    public String getOldTaskJson() {
        return oldTaskJson;
    }
    
    public void setOldTaskJson(String oldTaskJson) {
        this.oldTaskJson = oldTaskJson;
    }
    
    public String getNewTaskJson() {
        return newTaskJson;
    }
    
    public void setNewTaskJson(String newTaskJson) {
        this.newTaskJson = newTaskJson;
    }
    
    public String getTaskSnapshot() {
        return taskSnapshot;
    }
    
    public void setTaskSnapshot(String taskSnapshot) {
        this.taskSnapshot = taskSnapshot;
    }
} 