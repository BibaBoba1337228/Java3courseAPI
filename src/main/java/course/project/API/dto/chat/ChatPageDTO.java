package course.project.API.dto.chat;

import java.util.List;

public class ChatPageDTO {
    private List<ChatWithLastMessageDTO> chats;
    private boolean hasNext;

    public ChatPageDTO() {
    }

    public ChatPageDTO(List<ChatWithLastMessageDTO> chats, boolean hasNext) {
        this.chats = chats;
        this.hasNext = hasNext;
    }

    public List<ChatWithLastMessageDTO> getChats() {
        return chats;
    }

    public void setChats(List<ChatWithLastMessageDTO> chats) {
        this.chats = chats;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
