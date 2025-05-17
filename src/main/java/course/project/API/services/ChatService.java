package course.project.API.services;

import course.project.API.controllers.ChatWebSocketController;
import course.project.API.dto.chat.*;
import course.project.API.dto.chatSocket.ChatSocketEventDTO;
import course.project.API.dto.user.UserResponse;
import course.project.API.models.Chat;
import course.project.API.models.ChatRole;
import course.project.API.models.User;
import course.project.API.repositories.ChatRepository;
import course.project.API.repositories.MessageRepository;
import course.project.API.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final MessageRepository messageRepository;
    private final EntityManager entityManager;
    private final ChatWebSocketController chatWebSocketController;
    private final WebSocketService webSocketService;

    @Autowired
    public ChatService(ChatRepository chatRepository, UserRepository userRepository, ModelMapper modelMapper, MessageRepository messageRepository, EntityManager entityManager, ChatWebSocketController chatWebSocketController, WebSocketService webSocketService) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.messageRepository = messageRepository;
        this.entityManager = entityManager;
        this.chatWebSocketController = chatWebSocketController;
        this.webSocketService = webSocketService;
    }

    @Transactional
    public WrapperChatWithLastMessageDTOForController createPersonalChat(Long participantId, User currentUser) {
        logger.info("Создание персонального чата");
        if (chatRepository.existsPersonalChatBetweenUsers(currentUser.getId(), participantId) == 1) {
            logger.info("Личный чат между пользователями {} и {} уже существует", currentUser.getId(), participantId);
            return null;
        }
        Optional<User> participantOpt = userRepository.findById(participantId);
        if (participantOpt.isEmpty()) {
            return null;
        }
        User participant = participantOpt.get();
        Chat chat = new Chat();
        chat.setIsGroupChat(false);
        chat.getParticipants().add(currentUser);
        chat.getParticipants().add(participant);

        chat.getUserRoles().put(currentUser.getId(), ChatRole.MEMBER);
        chat.getUserRoles().put(participantId, ChatRole.MEMBER);

        Chat savedChat = chatRepository.save(chat);
        logger.info("Создан новый чат с ID: {}", savedChat.getId());
        ChatWithLastMessageDTO createdChat = new ChatWithLastMessageDTO(
                chat.getId(),
                participant.getName(),
                false,
                participant.getAvatarURL(),
                null
        );
        List<User> users = new ArrayList<>();
        users.add(participant);
        return new WrapperChatWithLastMessageDTOForController(createdChat, users);
    }

    @Transactional
    public WrapperChatWithLastMessageDTOForController createGroupChat(CreateGroupChatDTO chatRequest, User currentUser) {
        logger.info("Создание группового чата");

        Chat chat = new Chat();
        chat.setIsGroupChat(true);
        chat.setName(chatRequest.getName());
        chat.getParticipants().add(currentUser);
        chat.getUserRoles().put(currentUser.getId(), ChatRole.OWNER);
        List<User> users = new ArrayList<>(); // Для ответа

        if (chatRequest.getParticipantIds() != null) {
            List<User> participants = userRepository.findUsersByIds(chatRequest.getParticipantIds());
            logger.info("Участников: {}", chatRequest.getParticipantIds().size());

            if (participants.size() != chatRequest.getParticipantIds().size()) {
                logger.error("Не совпало число добавляемых пользователей пришло: {}, из базы взяли: {}", chatRequest.getParticipantIds().size(), participants.size());
                return null;
            }

            for (User user : participants) {
                logger.info("Добавляю в чат: {}", user.getId());
                users.add(user);
                chat.getParticipants().add(user);
                chat.getUserRoles().put(user.getId(), ChatRole.MEMBER);
            }
        }

        Chat savedChat = chatRepository.save(chat);
        logger.info("Создан новый чат с ID: {}", savedChat.getId());
        ChatWithLastMessageDTO createdChat = new ChatWithLastMessageDTO(
                chat.getId(),
                chat.getName(),
                true,
                null,
                null
        );

        return new WrapperChatWithLastMessageDTOForController(createdChat, users);
    }

    public Object[] getMessageAttachementFilePath(Long chatId, Long messageId, Long attachmentId) {
        return (Object[]) messageRepository.findMessageAttachementFilePathByChatIdAndMessageIdAndAttachementId(chatId, messageId, attachmentId);
    }

    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));
        chatRepository.delete(chat);
    }

    public boolean isParticipant(Long chatId, Long userId) {
        return chatRepository.existsByIdAndParticipantsId(chatId, userId);
    }

    @Transactional
    public void addParticipant(Long chatId, Long userId, ChatRole role) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        if (!chat.isGroupChat()) {
            throw new IllegalStateException("Cannot add participants to a direct chat");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (chat.getParticipants().contains(user)) {
            throw new IllegalStateException("User is already a participant");
        }

        chat.getParticipants().add(user);
        chat.getUserRoles().put(userId, role);
        chatRepository.save(chat);
    }

    @Transactional
    public void removeParticipant(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (!chat.getParticipants().contains(user)) {
            throw new IllegalStateException("User is not a participant");
        }

        if (chat.getUserRoles().get(userId) == ChatRole.OWNER) {
            throw new IllegalStateException("Cannot remove chat owner");
        }

        chat.getParticipants().remove(user);
        chat.getUserRoles().remove(userId);
        chatRepository.save(chat);
    }

    @Transactional
    public void setParticipantRole(Long chatId, Long userId, ChatRole newRole) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        if (!chat.getParticipants().stream().anyMatch(p -> p.getId().equals(userId))) {
            throw new IllegalStateException("User is not a participant");
        }

        if (chat.getUserRoles().get(userId) == ChatRole.OWNER) {
            throw new IllegalStateException("Cannot change owner's role");
        }

        chat.getUserRoles().put(userId, newRole);
        chatRepository.save(chat);
    }

    public Page<Chat> getPagedUserChats(Long userId, Pageable pageable) {
        return chatRepository.findByParticipantsId(userId, pageable);
    }

    public Page<ChatWithLastMessageDTO> getPagedChatsWithLastMessage(Long userId, Pageable pageable) {
        logger.info("Starting getPagedChatsWithLastMessage for userId: {}", userId);
        Page<Object[]> page = chatRepository.findChatsWithLastMessageByUserId(userId, pageable);
        logger.info("Query executed, processing {} results", page.getContent().size());
        Set<Long> chatIds = page.getContent().stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toSet());

        Map<Long, Chat> chatMap = chatRepository.findChatsByIdsWithParticipants(new ArrayList<>(chatIds)).stream()
                .collect(Collectors.toMap(Chat::getId, Function.identity()));

        List<ChatWithLastMessageDTO> chatsWithParticipants;
        List<ChatWithLastMessageDTO> chats = new ArrayList<ChatWithLastMessageDTO>();
        ChatWithLastMessageDTO dummyChat = new ChatWithLastMessageDTO();


        for (Object[] row : page) {
//            logger.info("Processing row: length={}", row.length);

            // c.id, c.name, c.is_group_chat,
            // m.id, m.content, m.created_at, m.is_edited,
            // u.id, u.name, u.avatar_url, mrb.user_id

            Long readedById = (Long) row[10];
            Long chatId = (Long) row[0];
            if (dummyChat.getId() != null && dummyChat.getId().equals(chatId)) {
                dummyChat.getLastMessage().getReadByIds().add(readedById);
                continue;
            }
//            logger.info("Новый чатик разбираю");
            dummyChat = new ChatWithLastMessageDTO();
            dummyChat.setId(chatId);
            dummyChat.setName((String) row[1]);
            dummyChat.setIsGroupChat((boolean) row[2]);

            if (!dummyChat.isGroupChat()) {
                try {
                    Chat chat = chatMap.get(chatId);

                    User companion = chat.getParticipants().stream()
                            .filter(u -> !u.getId().equals(userId))
                            .findFirst()
                            .orElse(null);

                    if (companion != null) {
                        dummyChat.setName(companion.getName());
                        dummyChat.setAvatarURL(companion.getAvatarURL());
//                        logger.info("Personal chat, using companion name: {}", chat.getName());
                    }
                } catch (Exception e) {
//                    logger.error("Error fetching participants for chat {}: {}", chatId, e.getMessage());
                }
            }

            Long messageId = row[3] != null ? (Long) row[3] : null;
            String messageContent = row[4] != null ? (String) row[4] : null;
            java.sql.Timestamp messageCreatedAt = row[5] != null ? (java.sql.Timestamp) row[5] : null;
            boolean messageIsEdited = row[6] != null ? (boolean) row[6] : false;

            Long senderId = row[7] != null ? (Long) row[7] : null;
            String senderName = row[8] != null ? (String) row[8] : null;
            String senderAvatarUrl = row[9] != null ? (String) row[9] : null;

//            logger.info("Extracted data: chatId={}, chatName={}, messageId={}, senderId={}",
//                    chatId, dummyChat.getName(), messageId, senderId);

            MessageDTO messageDTO = null;
            if (messageId != null) {
                UserResponse sender =
                        new course.project.API.dto.user.UserResponse(
                                senderId, senderName, senderAvatarUrl
                        );
                List<Long> readedByIds = new ArrayList<>();
                if (readedById != null) readedByIds.add(readedById);
                messageDTO = new MessageDTO(
                        messageId, chatId, sender, messageContent,
                        messageCreatedAt.toLocalDateTime(), messageIsEdited,
                        new ArrayList<>(), readedByIds
                );
            }

            dummyChat.setLastMessage(messageDTO);
            chats.add(dummyChat);
        }

        return new PageImpl<ChatWithLastMessageDTO>(chats, page.getPageable(), page.getTotalElements());
    }

    public Chat getChatWithParticipants(Long chatId) {
        return chatRepository.findByIdWithParticipants(chatId);
    }

    public ChatWithParticipantsDTO getChatWithParticipants(Long chatId, Long currentUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        String chatName = chat.getName();
        String chatAvatarURL = null;
        if (!chat.isGroupChat()) {
            User companion = chat.getParticipants().stream()
                    .filter(u -> !u.getId().equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (companion != null) {
                chatName = companion.getName();
                chatAvatarURL = companion.getAvatarURL();
            }
        }

        List<ParticipantDTO> participantDTOs = chat.getParticipants().stream()
                .map(user -> new ParticipantDTO(
                        user.getId(),
                        user.getName(),
                        user.getAvatarURL(),
                        chat.getUserRoles().get(user.getId())
                ))
                .collect(Collectors.toList());

        return new ChatWithParticipantsDTO(
                chat.getId(),
                chatName,
                chat.isGroupChat(),
                chatAvatarURL,
                participantDTOs
        );
    }


    // Групповые чаты напрямую содержат аву, а личные нет, надо достать имя и аватарку юзера и поставить их на место чата
    public ChatDTO getNormalizedChat(Chat chat, Long userId) {
        logger.info("Нормализую чат {}", chat.getId());
        ChatDTO chatDTO = new ChatDTO();
        chatDTO.setId(chat.getId());

        if (chat.isGroupChat()) {
            chatDTO.setName(chat.getName());
            chatDTO.setAvatarURL(null);
            return chatDTO;
        }

        User companion = chat.getParticipants().stream()
                .filter(u -> !u.getId().equals(userId))
                .findFirst()
                .orElse(null);

        if (companion != null) {
            chatDTO.setName(companion.getName());
            chatDTO.setAvatarURL(companion.getAvatarURL());
            logger.info("Personal chat, using companion name: {}", chatDTO.getName());
        } else {
            logger.error("Чето явно не так, в чате(" + chat.getId() + "не нашелся компаньон пользователя: " + userId);
            throw new EntityNotFoundException("Чето явно не так, в чате(" + chat.getId() + "не нашелся компаньон пользователя: " + userId);
        }
        return chatDTO;
    }

    public void notifyAddParticipant(Long chatId, UserResponse user, String username, Long initiatorId, ChatDTO chatDTO) {
        ChatSocketEventDTO event = chatWebSocketController.broadcastUserAdded(chatId, user, initiatorId);
        if (event != null) {
            webSocketService.sendPrivateMessageToUser(username, ChatSocketEventDTO.userAddedDirectPayload(chatDTO));
        }
    }

    public void notifyRemoveParticipant(Long chatId, Long userId, String participantUsername, Long initiatorId) {
        ChatSocketEventDTO event = chatWebSocketController.broadcastUserRemoved(chatId, userId, initiatorId);
        if (event != null) {
            webSocketService.sendPrivateMessageToUser(participantUsername, event);
        }
    }
}