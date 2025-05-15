package course.project.API.dto.board;

import java.util.Set;

public class ColumnWithTasksDTO {
    private Long id;
    private String name;
    private Long boardId;
    private Integer position;
    private Set<TaskDTO> tasks;
    private boolean isCompletionColumn;
    
    public ColumnWithTasksDTO() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Long getBoardId() {
        return boardId;
    }
    
    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }
    
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }
    
    public Set<TaskDTO> getTasks() {
        return tasks;
    }
    
    public void setTasks(Set<TaskDTO> tasks) {
        this.tasks = tasks;
    }
    
    public boolean isCompletionColumn() {
        return isCompletionColumn;
    }
    
    public void setCompletionColumn(boolean isCompletionColumn) {
        this.isCompletionColumn = isCompletionColumn;
    }
} 