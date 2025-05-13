package course.project.API.controllers;

import course.project.API.dto.chatSocket.ChatSocketEventDTO;
import course.project.API.dto.chat.MessageDTO;
import course.project.API.dto.chatSocket.MessageActionPayload;
import course.project.API.dto.chatSocket.UserActionPayload;
import course.project.API.dto.chatSocket.UserRoleChangedPayload;
import course.project.API.models.Chat;
import course.project.API.models.ChatRole;
import course.project.API.models.User;
import course.project.API.services.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @Autowired
    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    public void broadcastNewMessage(Long chatId, MessageDTO message) {
        try {
            ChatSocketEventDTO event = ChatSocketEventDTO.newMessage(chatId, message);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting new message for chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void broadcastUserRoleChanged(Long chatId, Long userId, String userName, ChatRole newRole) {
        try {
            UserRoleChangedPayload payload = new UserRoleChangedPayload(userId, userName, newRole);
            ChatSocketEventDTO event = ChatSocketEventDTO.userRoleChanged(chatId, payload);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting user role change for chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void broadcastUserRemoved(Long chatId, Long userId, String userName, String avatarURL) {
        try {
            UserActionPayload payload = new UserActionPayload(userId, userName, avatarURL);
            ChatSocketEventDTO event = ChatSocketEventDTO.userRemoved(chatId, payload);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting user removal from chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public void broadcastUserAdded(Long chatId, Long userId, String userName, String avatarURL) {
        try {
            UserActionPayload payload = new UserActionPayload(userId, userName, avatarURL);
            ChatSocketEventDTO event = ChatSocketEventDTO.userAdded(chatId, payload);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting user addition to chat {}: {}", chatId, e.getMessage(), e);
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

    public void broadcastMessageEdited(Long chatId, MessageDTO message) {
        try {
            ChatSocketEventDTO event = ChatSocketEventDTO.messageEdited(chatId, message);
            broadcastToChatParticipants(chatId, event);
        } catch (Exception e) {
            logger.error("Error broadcasting message edit for chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    private void broadcastToChatParticipants(Long chatId, ChatSocketEventDTO event) {
        try {
            if (chatId == null) {
                logger.error("Cannot broadcast to chat with null ID");
                return;
            }
            
            Chat chat = chatService.getChatEntityById(chatId);
            if (chat == null) {
                logger.error("Chat not found: {}", chatId);
                return;
            }

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