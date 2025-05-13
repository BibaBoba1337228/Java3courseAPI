package course.project.API.dto.chat;

import course.project.API.dto.user.UserResponse;
import java.time.LocalDateTime;
import java.util.List;

public class MessageDTO {
    private Long id;
    private Long chatId;
    private UserResponse sender;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;
    private boolean isEdited;
    private List<MessageAttachmentDTO> attachments;
    private List<Long> readByIds;
    private boolean isReaded;

    public MessageDTO() {
    }

    public MessageDTO(Long id, Long chatId, UserResponse sender, String content, LocalDateTime createdAt, boolean isEdited, List<MessageAttachmentDTO> attachments, List<Long> readByIds, boolean isReaded) {
        this.id = id;
        this.chatId = chatId;
        this.sender = sender;
        this.senderId = (sender != null) ? sender.getId() : null;
        this.content = content;
        this.createdAt = createdAt;
        this.isEdited = isEdited;
        this.attachments = attachments;
        this.readByIds = readByIds;
        this.isReaded = isReaded;
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

    public UserResponse getSender() {
        return sender;
    }

    public void setSender(UserResponse sender) {
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

    public void setIsEdited(boolean edited) {
        isEdited = edited;
    }

    public List<MessageAttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MessageAttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    public boolean isReaded() {
        return isReaded;
    }

    public void setIsReaded(boolean readed) {
        isReaded = readed;
    }

    public List<Long> getReadByIds() {
        return readByIds;
    }

    public void setReadByIds(List<Long> readByIds) {
        this.readByIds = readByIds;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

}