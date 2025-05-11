package course.project.API.controllers;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.chat.*;
import course.project.API.models.ChatRole;
import course.project.API.models.Message;
import course.project.API.models.User;
import course.project.API.services.ChatService;
import course.project.API.services.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final MessageService messageService;

    @Autowired
    public ChatController(ChatService chatService, MessageService messageService) {
        this.chatService = chatService;
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<ChatDTO> createChat(
            @RequestBody CreateChatRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            ChatDTO chat = chatService.createChat(request, currentUser);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            logger.error("Error creating chat: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        try {
            ChatRole role = chatService.getParticipantRole(chatId, currentUser.getId());
            if (role != ChatRole.OWNER) {
                return ResponseEntity.status(403).build();
            }
            chatService.deleteChat(chatId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting chat: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ChatDTO>> getMyChats(@AuthenticationPrincipal User currentUser) {
        try {
            List<ChatDTO> chats = chatService.getUserChats(currentUser.getId());
            return ResponseEntity.ok(chats);
        } catch (Exception e) {
            logger.error("Error getting user chats: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDTO> getChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }
            ChatDTO chat = chatService.getChatById(chatId);
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            logger.error("Error getting chat: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{chatId}/participants/{userId}")
    public ResponseEntity<?> addParticipant(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        try {
            ChatRole role = chatService.getParticipantRole(chatId, currentUser.getId());
            if (role != ChatRole.OWNER &&  role != ChatRole.MODERATOR) {
                return ResponseEntity.status(403).body(new SimpleDTO("You don't have permission to add participants"));
            }

            if (!chatService.isGroupChat(chatId)) {
                return ResponseEntity.badRequest().body(new SimpleDTO("Cannot add participants to a direct chat"));
            }

            chatService.addParticipant(chatId, userId, ChatRole.MEMBER);
            return ResponseEntity.ok(new SimpleDTO("Participant added successfully"));
        } catch (Exception e) {
            logger.error("Error adding participant: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @DeleteMapping("/{chatId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        try {
            ChatRole initiatorRole = chatService.getParticipantRole(chatId, currentUser.getId());
            ChatRole targetParticipantRole = chatService.getParticipantRole(chatId, userId);

            // Only owner can remove moderators
            if ((targetParticipantRole == ChatRole.MODERATOR) && initiatorRole != ChatRole.OWNER) {
                return ResponseEntity.status(403).body(new SimpleDTO("Only owner can remove admins and moderators"));
            }

            // Only owner or moderator can remove members
            if (targetParticipantRole == ChatRole.MEMBER && initiatorRole != ChatRole.OWNER && initiatorRole != ChatRole.MODERATOR) {
                return ResponseEntity.status(403).body(new SimpleDTO("You don't have permission to remove participants"));
            }

            // Owner cannot be removed
            if (targetParticipantRole == ChatRole.OWNER) {
                return ResponseEntity.badRequest().body(new SimpleDTO("Cannot remove chat owner"));
            }

            chatService.removeParticipant(chatId, userId);
            return ResponseEntity.ok(new SimpleDTO("Participant removed successfully"));
        } catch (Exception e) {
            logger.error("Error removing participant: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @PostMapping("/{chatId}/participants/{userId}/role")
    public ResponseEntity<?> changeParticipantRole(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @RequestBody ChatRole newRole,
            @AuthenticationPrincipal User currentUser) {
        try {
            ChatRole initiatorRole = chatService.getParticipantRole(chatId, currentUser.getId());
            ChatRole targetParticipantRole = chatService.getParticipantRole(chatId, userId);

            // Only owner can change roles
            if (initiatorRole != ChatRole.OWNER) {
                return ResponseEntity.status(403).body(new SimpleDTO("Only owner can change roles"));
            }

            // Cannot change owner's role
            if (targetParticipantRole == ChatRole.OWNER) {
                return ResponseEntity.badRequest().body(new SimpleDTO("Cannot change owner's role"));
            }

            // Cannot promote to owner
            if (newRole == ChatRole.OWNER) {
                return ResponseEntity.badRequest().body(new SimpleDTO("Cannot promote to owner"));
            }

            chatService.setParticipantRole(chatId, userId, newRole);
            return ResponseEntity.ok(new SimpleDTO("Role changed successfully"));
        } catch (Exception e) {
            logger.error("Error changing participant role: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable Long chatId,
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            MessageDTO message = messageService.sendMessage(chatId, currentUser.getId(), request);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{chatId}/messages/{messageId}/attachments")
    public ResponseEntity<?> uploadAttachment(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            Message message = messageService.getMessageById(messageId);
            if (message == null || !message.getChat().getId().equals(chatId)) {
                return ResponseEntity.notFound().build();
            }

            if (!message.getSender().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(new SimpleDTO("You can only attach files to your own messages"));
            }

            messageService.addAttachment(messageId, file, currentUser);
            return ResponseEntity.ok(new SimpleDTO("Attachment uploaded successfully"));
        } catch (Exception e) {
            logger.error("Error uploading attachment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @DeleteMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            Message message = messageService.getMessageById(messageId);
            if (message == null || !message.getChat().getId().equals(chatId)) {
                return ResponseEntity.notFound().build();
            }


            if (!message.getSender().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(new SimpleDTO("You don't have permission to delete this message"));
            }

            messageService.deleteMessage(messageId);
            return ResponseEntity.ok(new SimpleDTO("Message deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting message: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @PutMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<?> editMessage(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            Message message = messageService.getMessageById(messageId);
            if (message == null || !message.getChat().getId().equals(chatId)) {
                return ResponseEntity.notFound().build();
            }

            if (!message.getSender().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(new SimpleDTO("You can only edit your own messages"));
            }

            MessageDTO updatedMessage = messageService.editMessage(messageId, request);
            return ResponseEntity.ok(updatedMessage);
        } catch (Exception e) {
            logger.error("Error editing message: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @PostMapping("/{chatId}/messages/{messageId}/read")
    public ResponseEntity<?> markMessageAsRead(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            Message message = messageService.getMessageById(messageId);
            if (message == null || !message.getChat().getId().equals(chatId)) {
                return ResponseEntity.notFound().build();
            }

            messageService.markAsRead(messageId, currentUser.getId());
            return ResponseEntity.ok(new SimpleDTO("Message marked as read"));
        } catch (Exception e) {
            logger.error("Error marking message as read: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable Long chatId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            List<MessageDTO> messages = messageService.getChatMessages(chatId, page, size);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("Error getting messages: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 