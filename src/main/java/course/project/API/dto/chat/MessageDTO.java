package course.project.API.dto.chat;

import course.project.API.models.User;
import java.time.LocalDateTime;
import java.util.List;

public class MessageDTO {
    private Long id;
    private Long chatId;
    private User sender;
    private String content;
    private LocalDateTime createdAt;
    private boolean isEdited;
    private List<MessageAttachmentDTO> attachments;
    private List<User> readBy;

    public MessageDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }


    public List<MessageAttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MessageAttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    public List<User> getReadBy() {
        return readBy;
    }

    public void setReadBy(List<User> readBy) {
        this.readBy = readBy;
    }
} 