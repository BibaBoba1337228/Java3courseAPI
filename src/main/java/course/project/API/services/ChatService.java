package course.project.API.services;

import course.project.API.dto.chat.ChatDTO;
import course.project.API.dto.chat.CreateChatRequest;
import course.project.API.models.Chat;
import course.project.API.models.ChatRole;
import course.project.API.models.User;
import course.project.API.repositories.ChatRepository;
import course.project.API.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ChatService(ChatRepository chatRepository, UserRepository userRepository, ModelMapper modelMapper) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public ChatDTO createChat(CreateChatRequest request, User currentUser) {
        Chat chat = new Chat();
        chat.setName(request.getName());
        chat.setGroupChat(request.isGroupChat());
        chat.setAvatarURL(request.getAvatarURL());
        
        // Add current user as owner
        chat.getParticipants().add(currentUser);
        chat.getUserRoles().put(currentUser.getId(), ChatRole.OWNER);

        // Add other participants if provided
        if (request.getParticipantIds() != null) {
            for (Long userId : request.getParticipantIds()) {
                User participant = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
                chat.getParticipants().add(participant);
                chat.getUserRoles().put(userId, ChatRole.MEMBER);
            }
        }

        Chat savedChat = chatRepository.save(chat);
        return modelMapper.map(savedChat, ChatDTO.class);
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

}