package course.project.API.dto.chatSocket;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.chat.*;
import course.project.API.dto.user.UserResponse;

import java.time.LocalDateTime;

public class ChatSocketEventDTO {
    private String type;
    private Long chatId;
    private Object payload;
    private Long initiatorId;


    public ChatSocketEventDTO(String type, Long chatId, Object payload) {
        this.type = type;
        this.chatId = chatId;
        this.payload = payload;
    }

    public ChatSocketEventDTO(String type, Long chatId, Object payload, Long initiatorId) {
        this.type = type;
        this.chatId = chatId;
        this.payload = payload;
        this.initiatorId = initiatorId;
    }

    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    public static final String USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    public static final String USER_REMOVED = "USER_REMOVED";
    public static final String USER_ADDED = "USER_ADDED";
    public static final String MESSAGE_DELETED = "MESSAGE_DELETED";
    public static final String MESSAGE_EDITED = "MESSAGE_EDITED";
    public static final String MESSAGE_READED = "MESSAGE_READED";
    public static final String CHAT_CREATED = "CHAT_CREATED";
    public static final String CHAT_DELETED = "CHAT_DELETED";

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

    public Long getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(Long initiatorId) {
        this.initiatorId = initiatorId;
    }

    public static ChatSocketEventDTO newMessage(Long chatId, MessageDTO message) {
        return new ChatSocketEventDTO(NEW_MESSAGE, chatId, message);
    }

    public static ChatSocketEventDTO userRoleChanged(Long chatId, UserRoleChangedPayload payload, Long initiatorId) {
        return new ChatSocketEventDTO(USER_ROLE_CHANGED, chatId, payload, initiatorId);
    }

    public static ChatSocketEventDTO userRemoved(Long chatId, Long payload, Long initiatorId) {
        return new ChatSocketEventDTO(USER_REMOVED, chatId, payload, initiatorId);
    }

    public static ChatSocketEventDTO userAdded(Long chatId, UserResponse payload, Long initiatorId) {
        return new ChatSocketEventDTO(USER_ADDED, chatId, payload, initiatorId);
    }

    public static ChatSocketEventDTO userAddedDirectPayload(ChatDTO payload) {
        return new ChatSocketEventDTO(USER_ADDED, payload.getId(), payload);
    }

    public static ChatSocketEventDTO messageDeleted(Long chatId, MessageActionPayload payload) {
        return new ChatSocketEventDTO(MESSAGE_DELETED, chatId, payload);
    }

    public static ChatSocketEventDTO messageEdited(Long chatId, EditedMessageDTO message) {
        return new ChatSocketEventDTO(MESSAGE_EDITED, chatId, message);
    }

    public static ChatSocketEventDTO messageReaded(Long chatId, MessagesReadedDTO payload) {
        return new ChatSocketEventDTO(MESSAGE_READED, chatId, payload);
    }

    public static ChatSocketEventDTO chatDeleted(Long chatId, Long initiatorId) {
        return new ChatSocketEventDTO(CHAT_DELETED, chatId, initiatorId);
    }


    public static ChatSocketEventDTO chatCreated(ChatWithLastMessageDTO payload, Long initiatorId) {
        return new ChatSocketEventDTO(CHAT_CREATED, payload.getId(), payload, initiatorId);
    }
} 