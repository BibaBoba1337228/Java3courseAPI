package course.project.API.dto.chat;

public class EditedMessageDTO {
    private String content;
    private Long senderId;
    private Long messageId;

    public EditedMessageDTO() {
    }

    public EditedMessageDTO(String content, Long senderId, Long messageId) {
        this.content = content;
        this.senderId = senderId;
        this.messageId = messageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getSenderId() {
        return senderId;
    }
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }
}
