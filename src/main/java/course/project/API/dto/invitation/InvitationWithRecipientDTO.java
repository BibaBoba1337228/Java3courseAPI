package course.project.API.dto.invitation;

import course.project.API.dto.user.UserResponse;
import course.project.API.models.InvitationStatus;

import java.time.LocalDateTime;

public class InvitationWithRecipientDTO {
    private Long id;
    private Long senderId;
    private UserResponse recipient;
    private Long projectId;
    private InvitationStatus status;
    private LocalDateTime createdAt;

    public InvitationWithRecipientDTO() {
    }

    public InvitationWithRecipientDTO(Long id, Long senderId, UserResponse recipient, Long projectId, InvitationStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.recipient = recipient;
        this.projectId = projectId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public UserResponse getRecipient() {
        return recipient;
    }

    public void setRecipient(UserResponse recipient) {
        this.recipient = recipient;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
