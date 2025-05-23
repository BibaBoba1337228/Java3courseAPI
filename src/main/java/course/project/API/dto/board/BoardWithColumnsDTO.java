package course.project.API.dto.board;

import course.project.API.dto.user.UserResponse;
import java.util.Set;
import java.util.List;

public class BoardWithColumnsDTO {
    private Long id;
    private String title;
    private String description;
    private String emoji;
    private Long projectId;
    private Set<UserResponse> participants;
    private List<ColumnWithTasksDTO> columns;
    private Set<TagDTO> tags;
    private Double completionPercentage;
    
    public BoardWithColumnsDTO() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public Set<UserResponse> getParticipants() {
        return participants;
    }
    
    public void setParticipants(Set<UserResponse> participants) {
        this.participants = participants;
    }
    
    public List<ColumnWithTasksDTO> getColumns() {
        return columns;
    }
    
    public void setColumns(List<ColumnWithTasksDTO> columns) {
        this.columns = columns;
    }
    
    public Set<TagDTO> getTags() {
        return tags;
    }
    
    public void setTags(Set<TagDTO> tags) {
        this.tags = tags;
    }
    
    public Double getCompletionPercentage() {
        return completionPercentage;
    }
    
    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

} 