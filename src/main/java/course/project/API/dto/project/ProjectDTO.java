package course.project.API.dto.project;

import java.util.Set;

public class ProjectDTO {
    private Long id;
    private String title;
    private String description;
    private Set<Long> participantIds;
    private Long ownerId;

    public ProjectDTO() {
    }

    public ProjectDTO(Long id, String title, String description, Set<Long> participantIds, Long ownerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.participantIds = participantIds;
        this.ownerId = ownerId;
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

    public Set<Long> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(Set<Long> participantIds) {
        this.participantIds = participantIds;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
} 