package course.project.API.services;

import course.project.API.dto.project.ProjectDTO;
import course.project.API.dto.project.ProjectResponse;
import course.project.API.dto.user.UserResponse;
import course.project.API.models.Project;
import course.project.API.models.User;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.UserRepository;
import course.project.API.repositories.InvitationRepository;
import course.project.API.models.Invitation;
import course.project.API.models.InvitationStatus;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final ModelMapper modelMapper;
    private final ProjectRightService projectRightService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, InvitationRepository invitationRepository, ModelMapper modelMapper, ProjectRightService projectRightService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.modelMapper = modelMapper;
        this.projectRightService = projectRightService;
    }

    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<ProjectDTO> getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO, String username) {
        try {
            User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Project project = new Project(projectDTO.getTitle(), projectDTO.getDescription());
            project.setOwner(owner);
            
            if (projectDTO.getParticipants() != null) {
                Set<User> participants = new HashSet<>();
                for (String uname : projectDTO.getParticipants()) {
                    userRepository.findByUsername(uname).ifPresent(participants::add);
                }
                project.setParticipants(participants);
            }
            
            System.out.println("Saving project with title: " + projectDTO.getTitle() + " and owner: " + username);
            Project savedProject = projectRepository.save(project);
            
            try {
                // Grant all rights to the project owner
                System.out.println("Granting rights to owner for project ID: " + savedProject.getId());
                projectRightService.grantAllRightsToOwner(savedProject.getId());
            } catch (Exception e) {
                System.err.println("Error granting rights to owner: " + e.getMessage());
                e.printStackTrace();
                // Continue despite rights error - we don't want to lose the project itself
            }
            
            return convertToDTO(savedProject);
        } catch (Exception e) {
            System.err.println("Error creating project: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public Optional<ProjectDTO> updateProject(Long id, ProjectDTO projectDTO) {
        return projectRepository.findById(id)
                .map(project -> {
                    project.setTitle(projectDTO.getTitle());
                    project.setDescription(projectDTO.getDescription());
                    if (projectDTO.getParticipants() != null) {
                        Set<User> participants = new HashSet<>();
                        for (String uname : projectDTO.getParticipants()) {
                            userRepository.findByUsername(uname).ifPresent(participants::add);
                        }
                        project.setParticipants(participants);
                    }
                    return convertToDTO(projectRepository.save(project));
                });
    }

    @Transactional
    public void deleteProject(Long id) {
        try {
            Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
            
            // First remove all invitations (this has a foreign key constraint)
            invitationRepository.deleteAllByProject(project);
            
            // Clean up the project-participant relationship
            project.getParticipants().clear();
            
            // Delete the project
            projectRepository.delete(project);
            
            System.out.println("Project with ID " + id + " has been successfully deleted");
        } catch (Exception e) {
            System.err.println("Error deleting project: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public Optional<ProjectDTO> addParticipant(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .flatMap(project -> userRepository.findById(userId)
                        .map(user -> {
                            project.addParticipant(user);
                            return convertToDTO(projectRepository.save(project));
                        }));
    }

    @Transactional
    public Optional<ProjectDTO> removeParticipant(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .flatMap(project -> userRepository.findById(userId)
                        .map(user -> {
                            project.removeParticipant(user);
                            return convertToDTO(projectRepository.save(project));
                        }));
    }

    @Transactional
    public Optional<ProjectDTO> addParticipantByUsername(Long projectId, String username) {
        return projectRepository.findById(projectId)
                .flatMap(project -> userRepository.findByUsername(username)
                        .or(() -> userRepository.findByName(username))
                        .map(user -> {
                            // Check if invitation already exists
                            if (invitationRepository.existsByRecipientAndProjectAndStatus(user, project, InvitationStatus.PENDING)) {
                                throw new IllegalStateException("Pending invitation already exists");
                            }
                            
                            // Create new invitation
                            Invitation invitation = new Invitation(project.getOwner(), user, project);
                            invitationRepository.save(invitation);
                            
                            return convertToDTO(project);
                        }));
    }

    @Transactional
    public Optional<ProjectResponse> addParticipantByUsernameWithResponse(Long projectId, String username) {
        return projectRepository.findById(projectId)
                .flatMap(project -> userRepository.findByUsername(username)
                        .or(() -> userRepository.findByName(username))
                        .map(user -> {
                            // Check if invitation already exists
                            if (invitationRepository.existsByRecipientAndProjectAndStatus(user, project, InvitationStatus.PENDING)) {
                                throw new IllegalStateException("Pending invitation already exists");
                            }
                            
                            // Create new invitation
                            Invitation invitation = new Invitation(project.getOwner(), user, project);
                            invitationRepository.save(invitation);
                            
                            return convertToResponseDTO(project);
                        }));
    }

    @Transactional
    public Optional<ProjectDTO> removeParticipantByUsername(Long projectId, String username) {
        return projectRepository.findById(projectId)
                .flatMap(project -> userRepository.findByUsername(username)
                        .or(() -> userRepository.findByName(username))
                        .map(user -> {
                            // Remove user from project
                            project.removeParticipant(user);
                            
                            // Remove any pending invitations
                            invitationRepository.findByRecipientAndProjectAndStatus(user, project, InvitationStatus.PENDING)
                                    .ifPresent(invitation -> {
                                        invitation.setStatus(InvitationStatus.REJECTED);
                                        invitation.setUpdatedAt(LocalDateTime.now());
                                        invitationRepository.save(invitation);
                                    });
                            
                            return convertToDTO(projectRepository.save(project));
                        }));
    }

    public List<ProjectDTO> getMyProjects(Long userId) {
        return projectRepository.findByOwner_IdOrParticipants_Id(userId, userId)
            .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ProjectResponse> getMyProjectsWithUsers(Long userId) {
        return projectRepository.findByOwner_IdOrParticipants_Id(userId, userId)
            .stream().map(this::convertToResponseDTO).collect(Collectors.toList());
    }

    /**
     * Check if a user is the owner of a project
     * 
     * @param projectId Project ID
     * @param userId User ID
     * @return true if the user is the owner, false otherwise
     */
    public boolean isProjectOwner(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .map(project -> project.getOwner().getId().equals(userId))
                .orElse(false);
    }

    private ProjectDTO convertToDTO(Project project) {
        Set<String> participants = project.getParticipants().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());
        
        // Get pending invitations for this project
        Map<String, InvitationStatus> pendingInvitations = invitationRepository.findByProjectAndStatus(project, InvitationStatus.PENDING)
                .stream()
                .collect(Collectors.toMap(
                        invitation -> invitation.getRecipient().getUsername(),
                        Invitation::getStatus
                ));
        
        ProjectDTO projectDTO = modelMapper.map(project, ProjectDTO.class);
        projectDTO.setParticipants(participants);
        projectDTO.setPendingInvitations(pendingInvitations);
        return projectDTO;
    }

    private ProjectResponse convertToResponseDTO(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setTitle(project.getTitle());
        response.setDescription(project.getDescription());
        
        // Конвертируем владельца
        User owner = project.getOwner();
        UserResponse ownerResponse = new UserResponse(
            owner.getUsername(), 
            owner.getName(), 
            owner.getAvatarURL()
        );
        response.setOwner(ownerResponse);
        
        // Получаем все приглашения для этого проекта
        List<Invitation> pendingInvitations = invitationRepository.findByProjectAndStatus(project, InvitationStatus.PENDING);
        
        // Создаем Map с username в качестве ключа для быстрого поиска
        Map<String, InvitationStatus> invitationStatusMap = pendingInvitations.stream()
            .collect(Collectors.toMap(
                inv -> inv.getRecipient().getUsername(),
                Invitation::getStatus
            ));
        
        // Собираем существующих участников с меткой ACCEPTED
        Set<UserResponse> participantsWithStatus = project.getParticipants().stream()
            .map(user -> new UserResponse(
                user.getUsername(),
                user.getName(),
                user.getAvatarURL(),
                InvitationStatus.ACCEPTED
            ))
            .collect(Collectors.toSet());
        
        // Добавляем приглашенных пользователей
        pendingInvitations.forEach(invitation -> {
            User invitedUser = invitation.getRecipient();
            
            // Убедимся, что пользователь еще не является участником проекта
            boolean isAlreadyParticipant = project.getParticipants().stream()
                .anyMatch(participant -> participant.getUsername().equals(invitedUser.getUsername()));
            
            if (!isAlreadyParticipant) {
                participantsWithStatus.add(new UserResponse(
                    invitedUser.getUsername(),
                    invitedUser.getName(),
                    invitedUser.getAvatarURL(),
                    invitation.getStatus()
                ));
            }
        });
        
        response.setParticipants(participantsWithStatus);
        return response;
    }
} 