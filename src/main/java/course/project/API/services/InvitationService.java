package course.project.API.services;

import course.project.API.dto.invitation.InvitationDTO;
import course.project.API.models.Invitation;
import course.project.API.models.InvitationStatus;
import course.project.API.models.Project;
import course.project.API.models.User;
import course.project.API.repositories.InvitationRepository;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvitationService {
    private static final Logger logger = LoggerFactory.getLogger(InvitationService.class);

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public InvitationService(InvitationRepository invitationRepository,
                           UserRepository userRepository,
                           ProjectRepository projectRepository,
                           ModelMapper modelMapper) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public InvitationDTO sendInvitation(Long senderId, Long recipientId, Long projectId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        // Проверяем, не существует ли уже активное приглашение
        if (invitationRepository.existsByRecipientAndProjectAndStatus(recipient, project, InvitationStatus.PENDING)) {
            throw new IllegalStateException("Pending invitation already exists");
        }

        // Проверяем, не является ли пользователь уже участником проекта
        if (project.getParticipants().contains(recipient)) {
            throw new IllegalStateException("User is already a project participant");
        }

        Invitation invitation = new Invitation(sender, recipient, project);
        invitation = invitationRepository.save(invitation);
        
        return convertToDTO(invitation);
    }

    @Transactional
    public InvitationDTO acceptInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (!invitation.getRecipient().getId().equals(userId)) {
            throw new IllegalStateException("User is not the recipient of this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is not pending");
        }

        // Добавляем пользователя в проект
        Project project = invitation.getProject();
        User user = invitation.getRecipient();
        project.addParticipant(user);
        projectRepository.save(project);

        // Обновляем статус приглашения
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setUpdatedAt(LocalDateTime.now());
        invitation = invitationRepository.save(invitation);

        return convertToDTO(invitation);
    }

    @Transactional
    public InvitationDTO rejectInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

        if (!invitation.getRecipient().getId().equals(userId)) {
            throw new IllegalStateException("User is not the recipient of this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Invitation is not pending");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setUpdatedAt(LocalDateTime.now());
        invitation = invitationRepository.save(invitation);

        return convertToDTO(invitation);
    }

    public List<InvitationDTO> getUserInvitations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return invitationRepository.findByRecipient(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<InvitationDTO> getPendingInvitations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return invitationRepository.findByRecipientAndStatus(user, InvitationStatus.PENDING).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private InvitationDTO convertToDTO(Invitation invitation) {
        InvitationDTO dto = modelMapper.map(invitation, InvitationDTO.class);
        dto.setSenderId(invitation.getSender().getId());
        dto.setSenderUsername(invitation.getSender().getUsername());
        dto.setRecipientId(invitation.getRecipient().getId());
        dto.setRecipientUsername(invitation.getRecipient().getUsername());
        dto.setProjectId(invitation.getProject().getId());
        dto.setProjectTitle(invitation.getProject().getTitle());
        return dto;
    }
} 