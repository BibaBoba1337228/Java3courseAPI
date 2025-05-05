package course.project.API.dto.project;

import course.project.API.models.InvitationStatus;
import java.util.Map;
import java.util.Set;

public class ProjectDTO {
    private Long id;
    private String title;
    private String description;
    private Set<String> participants;
    private Long ownerId;
    private Map<String, InvitationStatus> pendingInvitations;

    public ProjectDTO() {
    }

    public ProjectDTO(Long id, String title, String description, Set<String> participants, Long ownerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.participants = participants;
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

    public Set<String> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<String> participants) {
        this.participants = participants;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Map<String, InvitationStatus> getPendingInvitations() {
        return pendingInvitations;
    }

    public void setPendingInvitations(Map<String, InvitationStatus> pendingInvitations) {
        this.pendingInvitations = pendingInvitations;
    }
} 