package course.project.API.dto.chat;

import java.util.List;

public class MessagePageDTO {
    private List<MessageDTO> messages;
    private boolean hasNext;


    public MessagePageDTO() {
    }

    public MessagePageDTO(List<MessageDTO> messages, boolean hasNext) {
        this.messages = messages;
        this.hasNext = hasNext;
    }

    public List<MessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageDTO> messages) {
        this.messages = messages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
} 