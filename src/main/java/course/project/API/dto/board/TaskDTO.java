package course.project.API.dto.board;

import course.project.API.dto.user.UserResponse;

import java.time.LocalDateTime;
import java.util.Set;

public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private Long columnId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Set<UserResponse> participants;
    private Integer position;
    private TagDTO tag;
    
    public TaskDTO() {
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
    
    public Long getColumnId() {
        return columnId;
    }
    
    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }

    public Set<UserResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<UserResponse> participants) {
        this.participants = participants;
    }

    public TagDTO getTag() {
        return tag;
    }

    public void setTag(TagDTO tag) {
        this.tag = tag;
    }
}