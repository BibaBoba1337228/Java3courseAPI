package course.project.API.dto.chatSocket;

public class MessageActionPayload {
    private Long messageId;
    private Long senderId;

    public MessageActionPayload() {
    }

    public MessageActionPayload(Long messageId, Long senderId) {
        this.messageId = messageId;
        this.senderId = senderId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
}
