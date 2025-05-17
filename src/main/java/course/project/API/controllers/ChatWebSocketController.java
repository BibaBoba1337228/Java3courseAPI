package course.project.API.controllers;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.chat.EditedMessageDTO;
import course.project.API.dto.chatSocket.*;
import course.project.API.dto.chat.MessageDTO;
import course.project.API.dto.user.UserResponse;
import course.project.API.models.ChatRole;
import course.project.API.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChatWebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public ChatSocketEventDTO broadcastNewMessage(Long chatId, MessageDTO message) {
        try {
            ChatSocketEventDTO event = ChatSocketEventDTO.newMessage(chatId, message);
            broadcastToChatParticipants(chatId, event);
            return event;
        } catch (Exception e) {
            logger.error("Error broadcasting new message for chat {}: {}", chatId, e.getMessage(), e);
            return null;
        }
    }
    public void broadcastMessagesReadedBy(Long chatId, List<Long> messageIds, Long readerId){
        try {
            MessagesReadedDTO payload = new MessagesReadedDTO(messageIds, readerId, chatId);
            ChatSocketEventDTO event = ChatSocketEventDTO.messageReaded(chatId, payload);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting new message for chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void broadcastUserRoleChanged(Long chatId, Long userId, ChatRole newRole, Long initiatorId) {
        try {
            UserRoleChangedPayload payload = new UserRoleChangedPayload(userId, newRole);
            ChatSocketEventDTO event = ChatSocketEventDTO.userRoleChanged(chatId, payload, initiatorId);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting user role change for chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public ChatSocketEventDTO broadcastUserRemoved(Long chatId, Long userId, Long initiatorId) {
        try {
            ChatSocketEventDTO event = ChatSocketEventDTO.userRemoved(chatId, userId, initiatorId);
            broadcastToChatParticipants(chatId, event);
            return event;
        } catch (Exception e) {
            logger.error("Error broadcasting user removal from chat {}: {}", chatId, e.getMessage(), e);
            return null;
        }
    }

    public ChatSocketEventDTO broadcastUserAdded(Long chatId, UserResponse user, Long initiatorId) {
        try {
            ChatSocketEventDTO event = ChatSocketEventDTO.userAdded(chatId, user, initiatorId);
            broadcastToChatParticipants(chatId, event);
            return event;
        } catch (Exception e) {
            logger.error("Error broadcasting user addition to chat {}: {}", chatId, e.getMessage(), e);
            return null;
        }
    }

    public void broadcastMessageDeleted(Long chatId, Long messageId, Long senderId) {
        try {
            MessageActionPayload payload = new MessageActionPayload(messageId, senderId);
            ChatSocketEventDTO event = ChatSocketEventDTO.messageDeleted(chatId, payload);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting message deletion for chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void broadcastMessageEdited(Long chatId, EditedMessageDTO message) {
        try {
            ChatSocketEventDTO event = ChatSocketEventDTO.messageEdited(chatId, message);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting message edit for chat {}: {}", chatId, e.getMessage(), e);
        }
    }


    private void broadcastToChatParticipants(Long chatId, ChatSocketEventDTO event) {
        try {

            String chatDestination = "/topic/chat/" + chatId;
            logger.debug("Broadcasting to {}: {}", chatDestination, event);
            messagingTemplate.convertAndSend(chatDestination, event);
        } catch (Exception e) {
            logger.error("Error broadcasting chat event: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat/{chatId}/connect")
    public void handleChatConnection(@DestinationVariable Long chatId, @AuthenticationPrincipal User currentUser) {
        logger.info("User {} connected to chat {}", currentUser.getId(), chatId);
    }

//    сообщения о печатании
//    @MessageMapping("/chat/{chatId}/typing")
//    public void handleTypingIndicator(@DestinationVariable Long chatId, @Payload Long userId) {
//        logger.debug("User {} is typing in chat {}", userId, chatId);
//
//        try {
//            User user = webSocketService.getUserById(userId);
//            if (user == null) {
//                return;
//            }
//
//            // Создаем упрощенный объект для события печатания
//            // Не используем полный ChatSocketEventDTO, чтобы уменьшить размер передаваемых данных
//            var typingEvent = new TypingIndicatorDTO(userId, user.getName(), chatId);
//
//            // Отправляем всем участникам чата
//            Chat chat = chatService.getChatEntityById(chatId);
//            if (chat == null) {
//                return;
//            }
//
//            for (User participant : chat.getParticipants()) {
//                if (!participant.getId().equals(userId)) { // Не отправляем самому себе
//                    String destination = "/topic/user/" + participant.getId() + "/chat";
//                    messagingTemplate.convertAndSend(destination, typingEvent);
//                }
//            }
//
//            // Также отправляем в общий канал чата
//            String chatDestination = "/topic/chat/" + chatId + "/typing";
//            messagingTemplate.convertAndSend(chatDestination, typingEvent);
//        } catch (Exception e) {
//            logger.error("Error sending typing indicator: {}", e.getMessage(), e);
//        }
//    }
} 