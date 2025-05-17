package course.project.API.controllers;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.chat.*;
import course.project.API.dto.chatSocket.ChatSocketEventDTO;
import course.project.API.dto.chatSocket.MessageReadedDTO;
import course.project.API.dto.user.UserResponse;
import course.project.API.models.*;
import course.project.API.repositories.ChatRepository;
import course.project.API.repositories.MessageRepository;
import course.project.API.repositories.UserRepository;
import course.project.API.services.ChatService;
import course.project.API.services.MessageService;
import course.project.API.services.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/chats")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final MessageService messageService;
    private final ChatRepository chatRepository;
    private final ChatWebSocketController chatWebSocketController;
    private final WebSocketService webSocketService;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Autowired
    public ChatController(ChatService chatService, MessageService messageService, ChatRepository chatRepository,
                          ChatWebSocketController chatWebSocketController, WebSocketService webSocketService,
                          MessageRepository messageRepository, UserRepository userRepository) {
        this.chatService = chatService;
        this.messageService = messageService;
        this.chatRepository = chatRepository;
        this.chatWebSocketController = chatWebSocketController;
        this.webSocketService = webSocketService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }


    @PostMapping("/personal")
    public ResponseEntity<?> createPersonalChat(
            @RequestBody Long userId,
            @AuthenticationPrincipal User currentUser) {
        try {

            WrapperChatWithLastMessageDTOForController chat = chatService.createPersonalChat(userId, currentUser);
            if (chat == null) {
                return ResponseEntity.status(400).body(new SimpleDTO("Такой чат уже есть"));
            }
            ChatSocketEventDTO event = ChatSocketEventDTO.chatCreated(chat.getChatWithLastMessageDTO(), currentUser.getId());

            webSocketService.sendPrivateMessageToUser(chat.getUsers().get(0).getUsername(), event);
            return ResponseEntity.ok(chat.getChatWithLastMessageDTO());
        } catch (Exception e) {
            logger.error("Error creating chat: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroupChat(
            @RequestBody CreateGroupChatDTO request,
            @AuthenticationPrincipal User currentUser) {
        try {
            WrapperChatWithLastMessageDTOForController chat = chatService.createGroupChat(request, currentUser);

            if (chat == null) {
                return ResponseEntity.status(400).body(new SimpleDTO("Добавляются фантомные пользователи"));
            }
            ChatSocketEventDTO event = ChatSocketEventDTO.chatCreated(chat.getChatWithLastMessageDTO(), currentUser.getId());
            for (User user : chat.getUsers()) {
                webSocketService.sendPrivateMessageToUser(user.getUsername(), event);
            }
            return ResponseEntity.ok(chat.getChatWithLastMessageDTO());
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
            ChatRole role = chatRepository.findChatRoleByChatIdAndUserId(chatId, currentUser.getId());
            if (role != ChatRole.OWNER) {
                return ResponseEntity.status(403).build();
            }

            Chat chat = chatRepository.findByIdWithParticipants(chatId);
            Set<User> participants = chat.getParticipants();
            chatService.deleteChat(chatId);

            for (User user : participants) {
                webSocketService.sendPrivateMessageToUser(user.getUsername(), ChatSocketEventDTO.chatDeleted(chatId, currentUser.getId()));
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting chat: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


    @PostMapping("/{chatId}/participants/{userId}")
    public ResponseEntity<?> addParticipant(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()){
                return ResponseEntity.status(404).body(new SimpleDTO("Пользователь не найден"));
            }
            User user = userOpt.get();
            ChatRole initiatorRole = chatRepository.findChatRoleByChatIdAndUserId(chatId, currentUser.getId());

            if (initiatorRole == null || initiatorRole == ChatRole.MEMBER) {
                return ResponseEntity.status(403).build();
            }

            ChatRole role = chatRepository.findChatRoleByChatIdAndUserId(chatId, userId);
            if (role != null) {
                return ResponseEntity.status(400).body(new SimpleDTO("Пользователь уже является учатсником чата"));
            }
            chatRepository.addParticipantMemberRole(chatId, userId);
            chatRepository.addParticipant(chatId, userId);
            Chat chat = chatRepository.findById(chatId).get();
            ChatDTO chatDTO = new ChatDTO();
            chatDTO.setName(chat.getName());
            chatDTO.setId(chatId);

            chatService.notifyAddParticipant(
                    chatId,
                    new UserResponse(
                            user.getId(),
                            user.getName(),
                            user.getAvatarURL()
                    ),
                    user.getUsername(),
                    currentUser.getId(),
                    chatDTO
            );

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

            ChatRole initiatorRole = chatRepository.findChatRoleByChatIdAndUserId(chatId, currentUser.getId());
            if (initiatorRole == null || initiatorRole == ChatRole.MEMBER) {
                return ResponseEntity.status(403).build();
            }

            List<Object[]> result = chatRepository.findChatRoleJoinUsernameByChatIdAndUserId(chatId, userId);
            if (result.isEmpty()) {
                return ResponseEntity.status(400).body("Нельзя выгнать того кого нету в чате");
            }

            ChatRole targetParticipantRole = ChatRole.valueOf((String) result.get(0)[0]);

            if ((targetParticipantRole == ChatRole.MODERATOR) && initiatorRole != ChatRole.OWNER) {
                return ResponseEntity.status(403).body(new SimpleDTO("Только владелец может выгнать модератора"));
            }

            if (targetParticipantRole == ChatRole.OWNER) {
                return ResponseEntity.badRequest().body(new SimpleDTO("Владельца нельзя выгнать =)"));
            }

            if (chatRepository.existsChatParticipantByChatIdAndUserId(chatId, userId) == 0) {
                return ResponseEntity.status(400).body(new SimpleDTO("Пользователь не состоит в чате"));
            }

            chatRepository.deleteParticipantByUserIdAndChatId(chatId, userId);
            chatRepository.deleteParticipantRightByUserIdAndChatId(chatId, userId);
            chatRepository.deleteMessagesByUserIdAndChatId(chatId, userId);
            chatService.notifyRemoveParticipant(chatId, userId, (String) result.get(0)[1], currentUser.getId());

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

            ChatRole initiatorRole = chatRepository.findChatRoleByChatIdAndUserId(chatId, currentUser.getId());
            if (initiatorRole == null) {
                return ResponseEntity.status(403).build();
            }

            if (initiatorRole != ChatRole.OWNER) {
                return ResponseEntity.status(403).body(new SimpleDTO("Только владелец может меняять роли"));
            }

            if (newRole == ChatRole.OWNER) {
                return ResponseEntity.badRequest().body(new SimpleDTO("Нельзя повысить пользователя до владельца"));
            }

            ChatRole targetUserRole = chatRepository.findChatRoleByChatIdAndUserId(chatId, currentUser.getId());

            if (targetUserRole == null) {
                return ResponseEntity.status(400).body(new SimpleDTO("Попытка изменить роль не участника чата"));
            }
            if (targetUserRole == newRole) {
                return ResponseEntity.status(400).body(new SimpleDTO("Пользователь уже обладает ролью " + newRole));
            }
            chatService.setParticipantRole(chatId, userId, newRole);

            chatWebSocketController.broadcastUserRoleChanged(chatId, userId, newRole, currentUser.getId());
            return ResponseEntity.ok(new SimpleDTO("Role changed successfully"));
        } catch (Exception e) {
            logger.error("Error changing participant role: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable Long chatId,
            @RequestBody SendMessageDTO request,
            @AuthenticationPrincipal User currentUser) {
        try {
            Chat chat = chatService.getChatWithParticipants(chatId);
            if (chat == null) {
                return ResponseEntity.status(404).build();
            }

            if (!chat.getParticipants().contains(currentUser)) {
                return ResponseEntity.status(403).build();
            }

            MessageDTO message = messageService.sendMessage(chatId, currentUser, request);
            ChatDTO chatDTO = chatService.getNormalizedChat(chat, currentUser.getId());
            message.setChat(chatDTO);
            ChatSocketEventDTO event = chatWebSocketController.broadcastNewMessage(chatId, message);

            for (User user : chat.getParticipants()) {
                webSocketService.sendPrivateMessageToUser(user.getUsername(), event);
            }

            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Error sending message: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{chatId}/messages/with-attachments")
    public ResponseEntity<MessageDTO> sendMessageWithAttachments(
            @PathVariable Long chatId,
            @RequestPart("content") String content,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal User currentUser) {
        try {
            Chat chat = chatService.getChatWithParticipants(chatId);
            if (chat == null) {
                return ResponseEntity.status(404).build();
            }
            if (!chat.getParticipants().contains(currentUser)) {
                return ResponseEntity.status(403).build();
            }


            MessageDTO message = messageService.sendMessageWithAttachments(chatId, currentUser, content, files);
            ChatDTO chatDTO = chatService.getNormalizedChat(chat, currentUser.getId());
            message.setChat(chatDTO);
            ChatSocketEventDTO event = chatWebSocketController.broadcastNewMessage(chatId, message);
            if (event != null) {
                for (User user : chat.getParticipants()) {
                    webSocketService.sendPrivateMessageToUser(user.getUsername(), event);
                }
            }
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Error sending message with attachments: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{chatId}/messages/{messageId}/attachments/{attachmentId}")
    public ResponseEntity<?> downloadAttachment(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }
            Object[] file_info = chatService.getMessageAttachementFilePath(chatId, messageId, attachmentId);
            if (file_info == null) {
                return ResponseEntity.badRequest().body(new SimpleDTO("Вложение не найдено"));
            }
            logger.info("file_info size: {}, file_info[0]: {}", file_info.length, file_info[0].toString());
            String filePath = (String) file_info[0];
            String fileName = (String) file_info[1];
            String fileType = (String) file_info[2];

            logger.info("filePath {}, fileName {}, fileType {}", filePath, fileName, fileType);

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ResponseEntity.badRequest().body(new SimpleDTO("Файл не найден на диске"));
            }
            Resource resource = new UrlResource(path.toUri());

            String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"")
                    .header("Content-Type", fileType)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error getting attachment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @DeleteMapping("/{chatId}/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @AuthenticationPrincipal User currentUser) {
        try {
            Message message = messageService.getMessageWithAttachmentsByMessageIdAndSenderIdAndChatId(messageId, currentUser.getId(), chatId);
            if (message == null) {
                return ResponseEntity.status(404).body(new SimpleDTO("Сообщение не найдено"));
            }

            messageService.deleteMessage(messageId);
            for (MessageAttachment attachment : message.getAttachments()) {
                try {
                    Path path = Paths.get(attachment.getFilePath());
                    Files.deleteIfExists(path);
                    logger.info("Удалил файл с диска: {}", attachment.getFilePath());
                } catch (IOException e) {
                    logger.error("Ошибка при удалении файла: {}, ошибка: {}", attachment.getFilePath(), e.getMessage());
                }
            }

            chatWebSocketController.broadcastMessageDeleted(chatId, messageId, currentUser.getId());

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
            @RequestBody SendMessageDTO request,
            @AuthenticationPrincipal User currentUser) {
        try {
            Message message = messageService.getMessageByMessageIdAndSenderIdAndChatId(messageId, currentUser.getId(), chatId);
            if (message == null) {
                return ResponseEntity.status(404).body(new SimpleDTO("Сообщение не найдено"));
            }
            messageService.editMessage(message, request);

            chatWebSocketController.broadcastMessageEdited(chatId, new EditedMessageDTO(message.getContent(), currentUser.getId(), messageId));

            return ResponseEntity.ok(new SimpleDTO("Сообщение обновлено"));
        } catch (Exception e) {
            logger.error("Error editing message: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @PostMapping("/{chatId}/read")
    public ResponseEntity<?> markMessagesAsRead(
            @PathVariable Long chatId,
            @RequestBody List<Long> messageIds,
            @AuthenticationPrincipal User currentUser) {
        try {

            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            if (messageIds.isEmpty()) {
                return ResponseEntity.status(400).body(new SimpleDTO("Малова то сообщений будет"));
            }
            List<Message> messages = messageRepository.findMessagesByChatIdAndIdsAndNotSentBy(chatId, messageIds, currentUser.getId());
            if (messages.size() != messageIds.size()) {
                return ResponseEntity.status(400).body(new SimpleDTO("Пользователь не может читать свои же сообщения"));
            }
            messageRepository.batchAddMessagesReadByUser(messageIds, currentUser.getId());

            chatWebSocketController.broadcastMessagesReadedBy(chatId, messageIds, currentUser.getId());
            Long messageId = messageRepository.findLastByChatId(chatId);
            if (messageId != null) {
                Optional<Message> messageOpt = messages.stream().filter(m -> Objects.equals(m.getId(), messageId)).findFirst();
                if (messageOpt.isPresent()) {
                    Message message = messageOpt.get();
                    MessageReadedDTO payload = new MessageReadedDTO(messageId, currentUser.getId(), chatId);
                    webSocketService.sendPrivateMessageToUser(message.getSender().getUsername(), new ChatSocketEventDTO(ChatSocketEventDTO.MESSAGE_READED, chatId, payload));
                }
            }

            return ResponseEntity.ok(new SimpleDTO("Messages marked as read"));
        } catch (Exception e) {
            logger.error("Error marking message as read: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new SimpleDTO(e.getMessage()));
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<ChatPageDTO> getPagedChats(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User currentUser) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        logger.info("Тут пока все окей: {}, {}", pageNumber, pageSize);

        var chatPage = chatService.getPagedChatsWithLastMessage(currentUser.getId(), pageRequest);

        return ResponseEntity.ok(new ChatPageDTO(chatPage.getContent(), chatPage.hasNext()));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatWithParticipantsDTO> getChatWithParticipants(
            @PathVariable Long chatId,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }
            ChatWithParticipantsDTO chat = chatService.getChatWithParticipants(chatId, currentUser.getId());
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            logger.error("Error getting chat with participants: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<MessagePageDTO> getPaginatedChatMessages(
            @PathVariable Long chatId,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!chatService.isParticipant(chatId, currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            Page<MessageDTO> messagePage = messageService.getChatMessagesWithOffset(chatId, offset, limit);

            MessagePageDTO response = new MessagePageDTO(
                    messagePage.getContent(),
                    messagePage.hasNext()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting paginated messages: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


}