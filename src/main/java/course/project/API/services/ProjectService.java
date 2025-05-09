package course.project.API.services;

import course.project.API.dto.project.ProjectDTO;
import course.project.API.dto.project.ProjectWithParticipantsOwnerDTO;
import course.project.API.dto.project.ProjectWithParticipantsOwnerInvitationsDTO;
import course.project.API.models.Project;
import course.project.API.models.User;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.UserRepository;
import course.project.API.repositories.InvitationRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                .map(project -> modelMapper.map(project, ProjectDTO.class))
                .collect(Collectors.toList());
    }

    public Optional<ProjectDTO> getProjectById(Long id) {
        return projectRepository.findById(id)
                .map(project -> modelMapper.map(project, ProjectDTO.class));
    }

    public Optional<ProjectWithParticipantsOwnerInvitationsDTO> getProjectWithParticipantsOwnerInvitationsById(Long id) {
        return projectRepository.findProjectWithParticipantsOwnerById(id)
                .map(project -> modelMapper.map(project, ProjectWithParticipantsOwnerInvitationsDTO.class));
    }
    public List<ProjectDTO> getMyProjects(Long userId) {
        return projectRepository.findByOwner_IdOrParticipants_Id(userId, userId)
                .stream().map(project -> modelMapper.map(project, ProjectDTO.class)).collect(Collectors.toList());
    }

    public List<ProjectWithParticipantsOwnerDTO> getMyProjectsWithUsers(Long userId) {
        return projectRepository.findByOwner_IdOrParticipants_Id(userId, userId)
                .stream().map(project -> modelMapper.map(project, ProjectWithParticipantsOwnerDTO.class)).collect(Collectors.toList());
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO, String username) {
        try {
            User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Project project = new Project(projectDTO.getTitle(), projectDTO.getDescription());
            project.setOwner(owner);

            
            System.out.println("Saving project with title: " + projectDTO.getTitle() + " and owner: " + username);
            Project savedProject = projectRepository.save(project);
            
            try {
                System.out.println("Granting rights to owner for project ID: " + savedProject.getId());
                projectRightService.grantAllRightsToOwner(savedProject.getId());
            } catch (Exception e) {
                System.err.println("Error granting rights to owner: " + e.getMessage());
                e.printStackTrace();
            }

            projectDTO.setId(savedProject.getId());
            return projectDTO;
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
                    return modelMapper.map(projectRepository.save(project), ProjectDTO.class);
                });
    }

    @Transactional
    public void deleteProject(Long id) {
        try {
            Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
            
            invitationRepository.deleteAllByProject(project);
            
            project.getParticipants().clear();
            
            projectRepository.delete(project);
            
            System.out.println("Project with ID " + id + " has been successfully deleted");
        } catch (Exception e) {
            System.err.println("Error deleting project: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public boolean addParticipant(Long projectId, Long userId) {
        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isEmpty()) {
                return false;
            }
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }
            
            Project project = projectOpt.get();
            User user = userOpt.get();
            
            project.addParticipant(user);
            return true;
        } catch (Exception e) {
            System.err.println("Error adding participant: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean removeParticipant(Long projectId, Long userId) {
        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isEmpty()) {
                return false;
            }
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }
            
            Project project = projectOpt.get();
            User user = userOpt.get();
            
            project.removeParticipant(user);

            return true;
        } catch (Exception e) {
            System.err.println("Error removing participant: " + e.getMessage());
            return false;
        }
    }


    public boolean isProjectOwner(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .map(project -> project.getOwner().getId().equals(userId))
                .orElse(false);
    }

}