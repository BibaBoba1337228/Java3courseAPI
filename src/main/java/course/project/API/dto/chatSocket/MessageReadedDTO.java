package course.project.API.dto.chatSocket;


public class MessageReadedDTO {
    private Long messageId;
    private Long readerId;
    private Long chatId;

    public MessageReadedDTO() {
    }

    public MessageReadedDTO(Long messageId, Long readerId, Long chatId) {
        this.messageId = messageId;
        this.readerId = readerId;
        this.chatId = chatId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getReaderId() {
        return readerId;
    }

    public void setReaderId(Long readerId) {
        this.readerId = readerId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}
