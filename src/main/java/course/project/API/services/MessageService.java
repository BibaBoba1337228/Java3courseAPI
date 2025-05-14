package course.project.API.services;

import course.project.API.dto.chat.MessageAttachmentDTO;
import course.project.API.dto.chat.MessageDTO;
import course.project.API.dto.chat.SendMessageDTO;
import course.project.API.dto.user.UserResponse;
import course.project.API.models.Chat;
import course.project.API.models.Message;
import course.project.API.models.MessageAttachment;
import course.project.API.models.User;
import course.project.API.repositories.ChatRepository;
import course.project.API.repositories.MessageAttachmentRepository;
import course.project.API.repositories.MessageRepository;
import course.project.API.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final ModelMapper modelMapper;
    private final String uploadDir = "uploads";
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    public MessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            UserRepository userRepository,
            MessageAttachmentRepository attachmentRepository, ModelMapper modelMapper) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.attachmentRepository = attachmentRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public MessageDTO sendMessage(Long chatId, User currentUser, SendMessageDTO request) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));


        Message message = new Message();
        message.setChat(chat);
        message.setSender(currentUser);
        message.setContent(request.getContent());
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        chatRepository.save(chat);
        MessageDTO msg = modelMapper.map(savedMessage, MessageDTO.class);
        msg.setSenderId(currentUser.getId());
        return msg;
    }

    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
            .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
    }


    public Message getMessageWithAttachmentsByMessageIdAndSenderIdAndChatId(Long messageId, Long senderId, Long chatId) {
        logger.info("Удаляю messageId: {}, senderId: {}, chatId: {}", messageId, senderId, chatId);
        return messageRepository.findMessageWithAttachmentsByChatIdAndSenderIdAndMessageId(chatId, senderId, messageId);
    }
    public Message getMessageByMessageIdAndSenderIdAndChatId(Long messageId, Long senderId, Long chatId) {
        logger.info("Удаляю messageId: {}, senderId: {}, chatId: {}", messageId, senderId, chatId);
        return messageRepository.findMessageByChatIdAndSenderIdAndMessageId(chatId, senderId, messageId);
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        messageRepository.deleteFullyByMessageId(messageId);
    }

    @Transactional
    public Message editMessage(Message message, SendMessageDTO request) {
        message.setContent(request.getContent());
        message.setIsEdited(true);
        return messageRepository.save(message);
    }

    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (!message.getReadBy().contains(user)) {
            message.getReadBy().add(user);
            messageRepository.save(message);
        }
    }

    public Page<MessageDTO> getChatMessages(Long chatId, Integer page, Integer size) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        
        logger.info("Executing query to get messages for chat {}", chatId);
        Page<Message> messages = messageRepository.findByChatIdWithAttachmentsAndSender(chatId, pageRequest);
        logger.info("Retrieved {} messages", messages.getContent().size());
        
        if (!messages.isEmpty()) {
            List<Long> messageIds = messages.getContent().stream()
                .map(Message::getId)
                .collect(Collectors.toList());
            
            logger.info("Loading readBy for {} messages", messageIds.size());
            List<Object[]> readByResults = messageRepository.loadReadByForMessages(messageIds);
            
            Map<Long, List<User>> readByMap = new HashMap<>();
            
            for (Object[] result : readByResults) {
                Long messageId = (Long) result[0];
                User user = (User) result[1];
                
                readByMap.computeIfAbsent(messageId, k -> new ArrayList<>()).add(user);
            }
            
            for (Message message : messages.getContent()) {
                if (!readByMap.containsKey(message.getId())) {
                    message.setReadBy(new ArrayList<>());
                    continue;
                }
                
                message.setReadBy(readByMap.get(message.getId()));
            }
        }
        
        return messages.map(message -> {
            logger.debug("Converting message {} to DTO", message.getId());
            MessageDTO dto = new MessageDTO();
            dto.setId(message.getId());
            dto.setChatId(chatId);
            dto.setContent(message.getContent());
            dto.setCreatedAt(message.getCreatedAt());
            dto.setIsEdited(message.isEdited());
            dto.setIsReaded(message.isReaded());
            
            if (message.getSender() != null) {
                dto.setSenderId(message.getSender().getId());
            }
            
            List<Long> readByIds = message.getReadBy().stream()
                .map(User::getId)
                .collect(Collectors.toList());
            dto.setReadByIds(readByIds);
            
            List<MessageAttachmentDTO> attachmentDTOs = message.getAttachments().stream()
                .map(attachment -> {
                    MessageAttachmentDTO attachmentDTO = new MessageAttachmentDTO();
                    attachmentDTO.setId(attachment.getId());
                    attachmentDTO.setFileName(attachment.getOriginalFileName());
                    attachmentDTO.setFileType(attachment.getFileType());
                    attachmentDTO.setFileSize(String.valueOf(attachment.getFileSize()));
                    attachmentDTO.setDownloadURL("/api/chats/" + chatId + "/messages/" + message.getId() +"/attachments/" + attachment.getId());
                    return attachmentDTO;
                })
                .collect(Collectors.toList());
            dto.setAttachments(attachmentDTOs);
            
            return dto;
        });
    }

    @Transactional
    public void addAttachment(Long messageId, MultipartFile file, User uploadedBy) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), filePath);

            MessageAttachment attachment = new MessageAttachment();
            attachment.setMessage(message);
            attachment.setFileName(filename);
            attachment.setOriginalFileName(file.getOriginalFilename());
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setUploadedBy(uploadedBy);
            attachment.setFilePath(filePath.toString());

            attachmentRepository.save(attachment);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }


    @Transactional
    public MessageDTO sendMessageWithAttachments(Long chatId, User sender, SendMessageDTO request, List<MultipartFile> files) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        
        if (files != null && !files.isEmpty()) {
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                for (MultipartFile file : files) {
                    if (file.isEmpty()) {
                        continue;
                    }
                    
                    String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    Path filePath = uploadPath.resolve(filename);
                    
                    Files.copy(file.getInputStream(), filePath);
                    
                    MessageAttachment attachment = new MessageAttachment();
                    attachment.setMessage(savedMessage);
                    attachment.setFileName(filename);
                    attachment.setOriginalFileName(file.getOriginalFilename());
                    attachment.setFileType(file.getContentType());
                    attachment.setFileSize(file.getSize());
                    attachment.setUploadedBy(sender);
                    attachment.setFilePath(filePath.toString());
                    
                    savedMessage.getAttachments().add(attachment);
                    attachmentRepository.save(attachment);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to process file uploads: " + e.getMessage(), e);
            }
        }
        
        savedMessage = messageRepository.save(savedMessage);
        
        MessageDTO dto = new MessageDTO();
        dto.setId(savedMessage.getId());
        dto.setChatId(chatId);
        dto.setContent(savedMessage.getContent());
        dto.setCreatedAt(savedMessage.getCreatedAt());
        dto.setIsEdited(savedMessage.isEdited());
        dto.setIsReaded(savedMessage.isReaded());
        
        dto.setSender(new UserResponse(
                sender.getId(),
                sender.getName(),
                sender.getAvatarURL()
        ));
        dto.setSenderId(sender.getId());
        
        List<MessageAttachmentDTO> attachmentDTOs = savedMessage.getAttachments().stream()
            .map(attachment -> {
                MessageAttachmentDTO attachmentDTO = new MessageAttachmentDTO();
                attachmentDTO.setId(attachment.getId());
                attachmentDTO.setFileName(attachment.getOriginalFileName());
                attachmentDTO.setFileType(attachment.getFileType());
                attachmentDTO.setFileSize(String.valueOf(attachment.getFileSize()));
                attachmentDTO.setDownloadURL("/api/chats/" + chatId + "/messages/" + message.getId() +"/attachments/" + attachment.getId());
                return attachmentDTO;
            })
            .collect(Collectors.toList());
        dto.setAttachments(attachmentDTOs);
        
        dto.setReadByIds(new ArrayList<>());
        
        return dto;
    }


    public Page<MessageDTO> getChatMessagesWithOffset(Long chatId, Integer offset, Integer limit) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        int offsetValue = offset != null ? offset : 0;
        int limitValue = limit != null ? limit : 20;
        
        int pageSize = limitValue;
        int pageNumber = offsetValue / pageSize;

        return getChatMessages(chatId, pageNumber, pageSize);
            
    }
} 