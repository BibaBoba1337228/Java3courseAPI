package course.project.API.config;

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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class AppConfig {
    
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        
        modelMapper.getConfiguration()
            .setMatchingStrategy(MatchingStrategies.STRICT)
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
        
        modelMapper.addConverter(new PersistentBagToSetConverter<>());
        
        modelMapper.createTypeMap(User.class, UserResponse.class);
        
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
        
        return modelMapper;
    }
} 