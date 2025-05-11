package course.project.API.config;

import course.project.API.dto.invitation.InvitationDTO;
import course.project.API.dto.invitation.InvitationWithRecipientDTO;
import course.project.API.dto.project.ProjectWithParticipantsOwnerInvitationsDTO;
import course.project.API.dto.user.UserResponse;
import course.project.API.models.Invitation;
import course.project.API.models.Project;
import course.project.API.models.User;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import course.project.API.dto.converters.PersistentBagToSetConverter;
import course.project.API.dto.chat.ChatDTO;
import course.project.API.dto.chat.MessageDTO;
import course.project.API.dto.chat.ChatWithLastMessageDTO;
import course.project.API.models.Chat;
import course.project.API.models.Message;
import course.project.API.models.ChatRole;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class AppConfig {
    
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        
        modelMapper.getConfiguration()
            .setMatchingStrategy(MatchingStrategies.STRICT)
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
            .setSkipNullEnabled(true);
        
        modelMapper.addConverter(new PersistentBagToSetConverter<>());
        
        modelMapper.createTypeMap(User.class, UserResponse.class);
        
        // Конвертер для InvitationDTO
        Converter<Invitation, InvitationDTO> simpleInvitationConverter = ctx -> {
            Invitation source = ctx.getSource();
            InvitationDTO destination = new InvitationDTO();
            
            destination.setId(source.getId());
            destination.setSenderId(source.getSender() != null ? source.getSender().getId() : null);
            destination.setRecipientId(source.getRecipient() != null ? source.getRecipient().getId() : null);
            destination.setProjectId(source.getProject() != null ? source.getProject().getId() : null);
            destination.setStatus(source.getStatus());
            destination.setCreatedAt(source.getCreatedAt());
            
            // Adding sender name and project title
            if (source.getSender() != null) {
                destination.setSenderName(source.getSender().getUsername());
            }
            
            if (source.getProject() != null) {
                destination.setProjectTitle(source.getProject().getTitle());
            }
            
            return destination;
        };
        
        modelMapper.createTypeMap(Invitation.class, InvitationDTO.class)
            .setConverter(simpleInvitationConverter);
        
        Converter<Invitation, InvitationWithRecipientDTO> invitationConverter = ctx -> {
            Invitation source = ctx.getSource();
            InvitationWithRecipientDTO destination = new InvitationWithRecipientDTO();
            
            destination.setId(source.getId());
            destination.setSenderId(source.getSender().getId());
            destination.setProjectId(source.getProject().getId());
            destination.setStatus(source.getStatus());
            destination.setCreatedAt(source.getCreatedAt());
            
            if (source.getRecipient() != null) {
                destination.setRecipient(modelMapper.map(source.getRecipient(), UserResponse.class));
            }
            
            return destination;
        };
        
        modelMapper.createTypeMap(Invitation.class, InvitationWithRecipientDTO.class)
            .setConverter(invitationConverter);
        
        Converter<Project, ProjectWithParticipantsOwnerInvitationsDTO> projectConverter = ctx -> {
            Project source = ctx.getSource();
            ProjectWithParticipantsOwnerInvitationsDTO destination = new ProjectWithParticipantsOwnerInvitationsDTO();
            
            destination.setId(source.getId());
            destination.setTitle(source.getTitle());
            destination.setDescription(source.getDescription());
            destination.setEmoji(source.getEmoji());
            
            if (source.getOwner() != null) {
                destination.setOwner(modelMapper.map(source.getOwner(), UserResponse.class));
            }
            
            if (source.getParticipants() != null && !source.getParticipants().isEmpty()) {
                Set<UserResponse> participants = source.getParticipants().stream()
                    .map(user -> modelMapper.map(user, UserResponse.class))
                    .collect(Collectors.toSet());
                destination.setParticipants(participants);
            }
            
            if (source.getInvitations() != null && !source.getInvitations().isEmpty()) {
                Set<InvitationWithRecipientDTO> invitations = source.getInvitations().stream()
                    .map(invitation -> modelMapper.map(invitation, InvitationWithRecipientDTO.class))
                    .collect(Collectors.toSet());
                destination.setInvitations(invitations);
            } else {
                destination.setInvitations(new HashSet<>());
            }
            
            return destination;
        };
        
        modelMapper.createTypeMap(Project.class, ProjectWithParticipantsOwnerInvitationsDTO.class)
            .setConverter(projectConverter);
        
        Converter<Chat, ChatDTO> chatConverter = ctx -> {
            Chat source = ctx.getSource();
            ChatDTO destination = new ChatDTO();
            
            destination.setId(source.getId());
            destination.setName(source.getName());
            destination.setIsGroupChat(source.isGroupChat());
            destination.setAvatarURL(source.getAvatarURL());
            
            if (source.getParticipants() != null) {
                Set<UserResponse> participants = source.getParticipants().stream()
                    .map(user -> modelMapper.map(user, UserResponse.class))
                    .collect(Collectors.toSet());
                destination.setParticipants(participants);
            }
            
            if (source.getUserRoles() != null) {
                Map<Long, ChatRole> roles = new HashMap<>();
                source.getUserRoles().forEach((userId, role) -> {
                    if (userId != null) {
                        roles.put(userId, role);
                    }
                });
                destination.setUserRoles(roles);
            }
            
            return destination;
        };
        
        modelMapper.createTypeMap(Chat.class, ChatDTO.class)
            .setConverter(chatConverter);
        
        // Converter<Message, MessageDTO> messageConverter = ctx -> {
        //     Message source = ctx.getSource();
        //     MessageDTO destination = new MessageDTO();
            
        //     destination.setId(source.getId());
        //     destination.setChatId(source.getChat().getId());
        //     destination.setContent(source.getContent());
        //     destination.setCreatedAt(source.getCreatedAt());
        //     destination.setEdited(source.isEdited());
            
        //     if (source.getSender() != null) {
        //         destination.setSender(modelMapper.map(source.getSender(), UserResponse.class));
        //     }
            
        //     if (source.getReadBy() != null) {
        //         destination.setReadBy(source.getReadBy().stream()
        //             .map(user -> modelMapper.map(user, UserResponse.class))
        //             .collect(Collectors.toList()));
        //     }
            
        //     if (source.getAttachments() != null) {
        //         destination.setAttachments(source.getAttachments().stream()
        //             .map(attachment -> modelMapper.map(attachment, MessageAttachmentDTO.class))
        //             .collect(Collectors.toList()));
        //     }
            
        //     return destination;
        // };
        
        // modelMapper.createTypeMap(Message.class, MessageDTO.class)
        //     .setConverter(messageConverter);
        
         return modelMapper;
    }
} 