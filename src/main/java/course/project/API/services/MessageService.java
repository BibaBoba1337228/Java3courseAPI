package course.project.API.services;

import course.project.API.dto.chat.MessageDTO;
import course.project.API.dto.chat.SendMessageRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final ModelMapper modelMapper;

    @Value("${app.upload.dir}")
    private String uploadDir;

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
    public MessageDTO sendMessage(Long chatId, Long senderId, SendMessageRequest request) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));
        
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + senderId));

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        // Update chat's last message timestamp
        chat.setLastMessageAt(savedMessage.getCreatedAt());
        chatRepository.save(chat);

        return modelMapper.map(savedMessage, MessageDTO.class);
    }

    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
            .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    @Transactional
    public MessageDTO editMessage(Long messageId, SendMessageRequest request) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        
        message.setContent(request.getContent());
        message.setEdited(true);

        Message savedMessage = messageRepository.save(message);
        return modelMapper.map(savedMessage, MessageDTO.class);
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

    public List<MessageDTO> getChatMessages(Long chatId, Integer page, Integer size) {
        Chat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
        
        return messageRepository.findByChatId(chatId, pageRequest).stream()
            .map(message -> modelMapper.map(message, MessageDTO.class))
            .collect(Collectors.toList());
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
            attachment.setUploadedAt(LocalDateTime.now());
            attachment.setFilePath(filePath.toString());

            attachmentRepository.save(attachment);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }


} 