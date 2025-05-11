package course.project.API.dto.chat;

public class ChatWithLastMessageDTO {
    private Long id;
    private String name;
    private boolean isGroupChat;
    private String avatarURL;
    private MessageDTO lastMessage;

    public ChatWithLastMessageDTO() {}

    public ChatWithLastMessageDTO(Long id, String name, boolean isGroupChat, String avatarURL, MessageDTO lastMessage) {
        this.id = id;
        this.name = name;
        this.isGroupChat = isGroupChat;
        this.avatarURL = avatarURL;
        this.lastMessage = lastMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isGroupChat() {
        return isGroupChat;
    }

    public void setGroupChat(boolean groupChat) {
        isGroupChat = groupChat;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public MessageDTO getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageDTO lastMessage) {
        this.lastMessage = lastMessage;
    }
} 