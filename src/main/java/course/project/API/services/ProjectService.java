package course.project.API.services;

import course.project.API.dto.project.ProjectDTO;
import course.project.API.dto.project.ProjectWithParticipantsOwnerDTO;
import course.project.API.dto.project.ProjectWithParticipantsOwnerInvitationsDTO;
import course.project.API.dto.user.UserResponse;
import course.project.API.models.Project;
import course.project.API.models.User;
import course.project.API.models.Board;
import course.project.API.models.DashBoardColumn;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.UserRepository;
import course.project.API.repositories.InvitationRepository;
import course.project.API.repositories.BoardRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final ModelMapper modelMapper;
    private final ProjectRightService projectRightService;
    private final BoardRepository boardRepository;
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, InvitationRepository invitationRepository, ModelMapper modelMapper, ProjectRightService projectRightService, BoardRepository boardRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.modelMapper = modelMapper;
        this.projectRightService = projectRightService;
        this.boardRepository = boardRepository;
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
    @Transactional(readOnly = true)
    public List<ProjectWithParticipantsOwnerDTO> getMyProjectsWithUsers(User currentUser) {
        logger.info("Достаю проекты с овнером и участниками");
        List<Project> projects = projectRepository.findByOwner_IdOrParticipants_Id(currentUser.getId(), currentUser.getId());
        logger.info("Достаю гига проекты");
        List<Project> gigaProjects = projectRepository.findProjectsByIdsWithBoardsColumnsTasks(
                projects.stream().map(Project::getId).toList()
        );
        logger.info("Делаю Map");
        Map<Long, Set<User>> projectsWithParticipants = projects.stream().collect(Collectors.toMap(Project::getId, Project::getParticipants));
        return gigaProjects
                .stream()
                .map(project -> {
                    ProjectWithParticipantsOwnerDTO dto = new ProjectWithParticipantsOwnerDTO();
                    dto.setId(project.getId());
                    dto.setTitle(project.getTitle());
                    dto.setDescription(project.getDescription());
                    dto.setEmoji(project.getEmoji());
                    dto.setParticipants(projectsWithParticipants.get(project.getId()).stream().map(user -> new UserResponse(
                           user.getId(),
                           user.getName(),
                            user.getAvatarURL()
                    )).collect(Collectors.toSet()));
                    dto.setOwner(new UserResponse(
                            project.getOwner().getId(),
                            project.getOwner().getName(),
                            project.getOwner().getAvatarURL()
                    ));
                    logger.info("Считаю комплит");

                    dto.setCompletionPercentage(calculateProjectCompletionPercentage(project));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO, String username) {
        try {
            User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Project project = new Project(
                projectDTO.getTitle(), 
                projectDTO.getDescription(),
                projectDTO.getEmoji(),
                owner
            );

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
                    project.setEmoji(projectDTO.getEmoji());
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
            
            for (Board board : project.getBoards()) {
                board.removeParticipant(user);
            }
            
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

    private Double calculateProjectCompletionPercentage(Project project) {
        Set<Board> boards = project.getBoards();
        if (boards.isEmpty()) {
            return 0.0;
        }
        
        double totalPercentage = 0.0;
        
        for (Board board : boards) {
            totalPercentage += calculateBoardCompletionPercentage(board);
        }
        
        double averagePercentage = totalPercentage / boards.size();
        return Math.round(averagePercentage * 100.0) / 100.0;
    }
    

    private Double calculateBoardCompletionPercentage(Board board) {
        int totalTasks = 0;
        int completedTasks = 0;
        
        for (DashBoardColumn column : board.getColumns()) {
            int columnTaskCount = column.getTasks().size();
            totalTasks += columnTaskCount;
            
            // If this column is marked as completion column, count all its tasks as completed
            if (column.isCompletionColumn()) {
                completedTasks += columnTaskCount;
            }
        }
        
        // Avoid division by zero
        if (totalTasks == 0) {
            return 0.0;
        }
        
        // Calculate percentage and round to 2 decimal places
        double percentage = (double) completedTasks / totalTasks * 100.0;
        return Math.round(percentage * 100.0) / 100.0;
    }

}