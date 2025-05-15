package course.project.API.services;

import course.project.API.dto.chat.ChatDTO;
import course.project.API.dto.chat.CreateChatDTO;
import course.project.API.dto.chat.ChatWithLastMessageDTO;
import course.project.API.dto.chat.MessageDTO;
import course.project.API.dto.chat.ChatWithParticipantsDTO;
import course.project.API.dto.chat.ParticipantDTO;
import course.project.API.models.Chat;
import course.project.API.models.ChatRole;
import course.project.API.models.User;
import course.project.API.repositories.ChatRepository;
import course.project.API.repositories.MessageRepository;
import course.project.API.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final MessageRepository messageRepository;

    @Autowired
    public ChatService(ChatRepository chatRepository, UserRepository userRepository, ModelMapper modelMapper, MessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ChatDTO createChat(CreateChatDTO request, User currentUser) {
        logger.info("Создание чата: {}, isGroupChat: {}", request.getName(), request.isGroupChat());
        if (!request.isGroupChat() && request.getParticipantIds().size() > 1) {
            logger.error("Кто то создает личный чат с многими пользователями");
            return null;
        }
        if (!request.isGroupChat() && request.getParticipantIds().size() == 1) {
            Long otherUserId = request.getParticipantIds().get(0);
            logger.info("Нашел пользователя {}", otherUserId);
            if (chatRepository.existsPersonalChatBetweenUsers(currentUser.getId(), otherUserId) == 1) {
                logger.info("Личный чат между пользователями {} и {} уже существует", currentUser.getId(), otherUserId);
                return null;

            }
        }
        
        Chat chat = new Chat();
        chat.setName(request.getName());
        chat.setIsGroupChat(request.isGroupChat());
        chat.setAvatarURL(request.getAvatarURL());
        
        chat.getParticipants().add(currentUser);
        chat.getUserRoles().put(currentUser.getId(), ChatRole.OWNER);

        if (request.getParticipantIds() != null) {
            for (Long userId : request.getParticipantIds()) {
                User participant = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
                chat.getParticipants().add(participant);
                chat.getUserRoles().put(userId, ChatRole.MEMBER);
            }
        }

        Chat savedChat = chatRepository.save(chat);
        logger.info("Создан новый чат с ID: {}", savedChat.getId());
        return modelMapper.map(savedChat, ChatDTO.class);
    }

    public Object[] getMessageAttachementFilePath(Long chatId, Long messageId, Long attachmentId){
        return (Object[])messageRepository.findMessageAttachementFilePathByChatIdAndMessageIdAndAttachementId(chatId, messageId, attachmentId);
    }

    @Transactional
    public void deleteChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));
        chatRepository.delete(chat);
    }

    public List<ChatDTO> getUserChats(Long userId) {
        List<Chat> chats = chatRepository.findByParticipantsId(userId);
        return chats.stream()
            .map(chat -> modelMapper.map(chat, ChatDTO.class))
            .collect(Collectors.toList());
    }

    public ChatDTO getChatById(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));
        return modelMapper.map(chat, ChatDTO.class);
    }

    public boolean isParticipant(Long chatId, Long userId) {
        return chatRepository.existsByIdAndParticipantsId(chatId, userId);
    }

    public ChatRole getParticipantRole(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));
        return chat.getUserRoles().getOrDefault(userId, null);
    }

    public boolean isGroupChat(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));
        return chat.isGroupChat();
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
        
        return page.map(row -> {
            logger.info("Processing row: length={}", row.length);
            
            // c.id, c.name, c.is_group_chat, c.avatar_url, 
            // m.id, m.content, m.created_at, m.is_edited,
            // u.id, u.name, u.avatar_url
            
            Long chatId = (Long) row[0];
            String chatName = (String) row[1];;
            boolean isGroupChat = (boolean) row[2];
            String chatAvatarUrl = (String) row[3];
            

            if (!isGroupChat) {
                try {
                    Chat chat = chatRepository.findById(chatId).get();
                    

                    User companion = chat.getParticipants().stream()
                            .filter(u -> !u.getId().equals(userId))
                            .findFirst()
                            .orElse(null);

                    if (companion != null) {
                        chatName = companion.getName();
                        chatAvatarUrl = companion.getAvatarURL();
                        logger.info("Personal chat, using companion name: {}", chatName);
                    }
                } catch (Exception e) {
                    logger.error("Error fetching participants for chat {}: {}", chatId, e.getMessage());
                }
            }
            
            Long messageId = row[4] != null ? (Long) row[4] : null;
            String messageContent = row[5] != null ? (String) row[5] : null;
            java.sql.Timestamp messageCreatedAt = row[6] != null ? (java.sql.Timestamp) row[6] : null;
            boolean messageIsEdited = row[7] != null ? (boolean) row[7] : false;

            Long senderId = row[8] != null ? (Long) row[8] : null;
            String senderName = row[9] != null ? (String) row[9] : null;
            String senderAvatarUrl = row[10] != null ? (String) row[10] : null;
            
            logger.info("Extracted data: chatId={}, chatName={}, messageId={}, senderId={}", 
                       chatId, chatName, messageId, senderId);
            
            MessageDTO messageDTO = null;
            if (messageId != null) {
                course.project.API.dto.user.UserResponse sender =
                        new course.project.API.dto.user.UserResponse(
                                senderId, senderName, senderAvatarUrl
                        );

                messageDTO = new MessageDTO(
                        messageId, chatId, sender, messageContent, messageCreatedAt.toLocalDateTime(), messageIsEdited, new ArrayList<>(), new ArrayList<>()
                );
            }
            
            return new ChatWithLastMessageDTO(
                chatId,
                chatName,
                isGroupChat,
                chatAvatarUrl,
                messageDTO
            );
        });
    }
    public Chat getChatWithParticipants(Long chatId) {
        return chatRepository.findByIdWithParticipants(chatId);
    }

    public ChatWithParticipantsDTO getChatWithParticipants(Long chatId, Long currentUserId) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));
        
        String chatName = chat.getName();
        String chatAvatarURL = chat.getAvatarURL();
        
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

    public Chat getChatEntityById(Long chatId) {
        return chatRepository.findById(chatId).orElse(null);
    }


    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    // Групповые чаты напрямую содержат аву, а личные нет, надо достать имя и аватарку юзера и поставить их на место чата
    public ChatDTO getNormalizedChat(Chat chat, Long userId) {
        logger.info("Нормализую чат {}", chat.getId());
        ChatDTO chatDTO = new ChatDTO();
        chatDTO.setId(chat.getId());

        if (chat.isGroupChat()){
            chatDTO.setName(chat.getName());
            chatDTO.setAvatarURL(chat.getAvatarURL());
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
}