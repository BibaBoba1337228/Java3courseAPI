package course.project.API.dto.chatSocket;

import course.project.API.dto.chat.MessageDTO;
import java.time.LocalDateTime;

public class ChatSocketEventDTO {
    private String type;
    private Long chatId;
    private Object payload;
    private LocalDateTime timestamp;

    public ChatSocketEventDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatSocketEventDTO(String type, Long chatId, Object payload) {
        this.type = type;
        this.chatId = chatId;
        this.payload = payload;
    }

    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    public static final String USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    public static final String USER_REMOVED = "USER_REMOVED";
    public static final String USER_ADDED = "USER_ADDED";
    public static final String MESSAGE_DELETED = "MESSAGE_DELETED";
    public static final String MESSAGE_EDITED = "MESSAGE_EDITED";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }


    public static ChatSocketEventDTO newMessage(Long chatId, MessageDTO message) {
        return new ChatSocketEventDTO(NEW_MESSAGE, chatId, message);
    }

    public static ChatSocketEventDTO userRoleChanged(Long chatId, UserRoleChangedPayload payload) {
        return new ChatSocketEventDTO(USER_ROLE_CHANGED, chatId, payload);
    }

    public static ChatSocketEventDTO userRemoved(Long chatId, UserActionPayload payload) {
        return new ChatSocketEventDTO(USER_REMOVED, chatId, payload);
    }

    public static ChatSocketEventDTO userAdded(Long chatId, UserActionPayload payload) {
        return new ChatSocketEventDTO(USER_ADDED, chatId, payload);
    }

    public static ChatSocketEventDTO messageDeleted(Long chatId, MessageActionPayload payload) {
        return new ChatSocketEventDTO(MESSAGE_DELETED, chatId, payload);
    }

    public static ChatSocketEventDTO messageEdited(Long chatId, MessageDTO message) {
        return new ChatSocketEventDTO(MESSAGE_EDITED, chatId, message);
    }


} 