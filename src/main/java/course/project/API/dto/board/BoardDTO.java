package course.project.API.dto.board;

import java.util.List;
import java.util.Set;

public class BoardDTO {
    private Long id;
    private String title;
    private String description;
    private Long projectId;
    private Set<Long> participantIds;
    private List<TagDTO> tags;

    public BoardDTO() {
    }

    public BoardDTO(Long id, String title, String description, Long projectId, Set<Long> participantIds) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.projectId = projectId;
        this.participantIds = participantIds;
    }

    public BoardDTO(Long id, String title, String description, Long projectId, Set<Long> participantIds, List<TagDTO> tags) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.projectId = projectId;
        this.participantIds = participantIds;
        this.tags = tags;
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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Set<Long> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(Set<Long> participantIds) {
        this.participantIds = participantIds;
    }
    
    public List<TagDTO> getTags() {
        return tags;
    }
    
    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }
} 