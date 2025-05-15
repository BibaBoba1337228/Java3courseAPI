package course.project.API.dto.chatSocket;

import java.util.List;

public class MessagesReadedDTO {
    private List<Long> messagesIds;
    private Long readerId;
    private Long chatId;

    public MessagesReadedDTO() {
    }

    public MessagesReadedDTO(List<Long> messagesIds, Long readerId, Long chatId) {
        this.messagesIds = messagesIds;
        this.readerId = readerId;
        this.chatId = chatId;
    }

    public List<Long> getMessagesIds() {
        return messagesIds;
    }

    public void setMessagesIds(List<Long> messagesIds) {
        this.messagesIds = messagesIds;
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
