package course.project.API.dto.board;

import course.project.API.dto.user.UserResponse;
import course.project.API.models.TaskHistory;
import course.project.API.models.User;

import java.time.LocalDateTime;

public class TaskHistoryDTO {
    private Long id;
    private String actionType;
    private LocalDateTime timestamp;
    private UserResponse user;
    private Long taskId;
    private Long boardId;
    private String oldTaskJson;
    private String newTaskJson;
    private String taskSnapshot;

    public TaskHistoryDTO() {
    }

    public TaskHistoryDTO(TaskHistory history) {
        this.id = history.getId();
        this.actionType = history.getActionType();
        this.timestamp = history.getTimestamp();
        this.taskId = history.getTaskId();
        this.boardId = history.getBoardId();
        this.oldTaskJson = history.getOldTaskJson();
        this.newTaskJson = history.getNewTaskJson();
        this.taskSnapshot = history.getTaskSnapshot();
        
        if (history.getUser() != null) {
            User user = history.getUser();
            this.user = new UserResponse(user.getId(), user.getName(), user.getAvatarURL());
        }
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

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
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