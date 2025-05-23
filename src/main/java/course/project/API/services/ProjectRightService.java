package course.project.API.services;

import course.project.API.models.Board;
import course.project.API.models.BoardRight;
import course.project.API.models.Project;
import course.project.API.models.ProjectRight;
import course.project.API.models.ProjectUserRight;
import course.project.API.models.User;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.ProjectUserRightRepository;
import course.project.API.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
public class ProjectRightService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectUserRightRepository projectUserRightRepository;
    private final BoardService boardService;
    private final EntityManager entityManager;
    @Autowired
    public ProjectRightService(ProjectRepository projectRepository,
                               UserRepository userRepository,
                               ProjectUserRightRepository projectUserRightRepository,
                               @Lazy BoardService boardService, EntityManager entityManager) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectUserRightRepository = projectUserRightRepository;
        this.boardService = boardService;
        this.entityManager = entityManager;
    }

    /**
     * Add a user to a project with VIEW_PROJECT right only.
     * The user is not automatically added to any boards.
     */
    @Transactional
    public void addUserToProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Add user as participant to the project
        project.addParticipant(user);
        
        // By default, give VIEW_PROJECT right only
        grantProjectRight(projectId, userId, ProjectRight.VIEW_PROJECT);
        
        projectRepository.save(project);
    }
    
    /**
     * Grant a specific right to a user in a project
     */
    @Transactional
    public void grantProjectRight(Long projectId, Long userId, ProjectRight right) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Check if user is participant
        if (!project.getParticipants().contains(user)) {
            project.addParticipant(user);
        }
        
        // Check if right already exists
        if (projectUserRightRepository.findByProjectAndUserAndRight(project, user, right).isEmpty()) {
            project.addUserRight(user, right);
            projectRepository.save(project);
            
            // If the ACCESS_ALL_BOARDS right is granted, add the user to all boards in the project
            if (right == ProjectRight.ACCESS_ALL_BOARDS) {
                boardService.addUserToAllProjectBoards(projectId, userId);
            }

            // If MANAGE_BOARD_RIGHTS or MANAGE_ACCESS is granted, give MANAGE_RIGHTS on all boards where user is participant
            if (right == ProjectRight.MANAGE_BOARD_RIGHTS) {
                // Get all boards in this project
                Set<Board> projectBoards = project.getBoards();
                
                // For each board where user is a participant, grant MANAGE_RIGHTS
                for (Board board : projectBoards) {
                    if (board.getParticipants().contains(user)) {
                        board.addUserRight(user, BoardRight.MANAGE_RIGHTS);
                    }
                }
            }

            if (right == ProjectRight.MANAGE_ACCESS) {
                Set<Board> projectBoards = project.getBoards();
                
                for (Board board : projectBoards) {
                    if (board.getParticipants().contains(user)) {
                        board.addUserRight(user, BoardRight.MANAGE_MEMBERS);
                    }
                }
            }

        }
    }
    
    /**
     * Revoke a specific right from a user in a project
     */
    @Transactional
    public void revokeProjectRight(Long projectId, Long userId, ProjectRight right) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Cannot revoke rights from project owner
        if (project.getOwner().equals(user)) {
            throw new RuntimeException("Cannot revoke rights from project owner");
        }
        
        project.removeUserRight(user, right);
        projectRepository.save(project);
        
        // If the ACCESS_ALL_BOARDS right is revoked, remove user from all boards in the project
        if (right == ProjectRight.ACCESS_ALL_BOARDS) {
            boardService.removeUserFromAllProjectBoards(projectId, userId);
        }
        
        // If MANAGE_BOARD_RIGHTS or MANAGE_ACCESS is revoked, remove MANAGE_RIGHTS from all boards
        if (right == ProjectRight.MANAGE_BOARD_RIGHTS) {
            // Get all boards in this project
            Set<Board> projectBoards = project.getBoards();
            
            // Check if the user still has any of these rights at project level
            boolean stillHasRights = hasProjectRight(projectId, userId, ProjectRight.MANAGE_BOARD_RIGHTS);
            
            // If they don't have either right at project level anymore, remove the board-level right
            if (!stillHasRights) {
                // For each board, remove MANAGE_RIGHTS
                for (Board board : projectBoards) {
                    board.removeUserRight(user, BoardRight.MANAGE_RIGHTS);
                }
            }
        }

        if (right == ProjectRight.MANAGE_ACCESS) {
            // Get all boards in this project
            Set<Board> projectBoards = project.getBoards();
            
            boolean stillHasRights = hasProjectRight(projectId, userId, ProjectRight.MANAGE_ACCESS);
            
            if (!stillHasRights) {
                for (Board board : projectBoards) {
                    board.removeUserRight(user, BoardRight.MANAGE_MEMBERS);
                }
            }
        }
    }
    
    /**
     * Get all rights for a user in a project
     */
    public Set<ProjectRight> getUserProjectRights(Long projectId, Long userId) {
        List<ProjectUserRight> userRights = projectUserRightRepository.findByProjectIdAndUserId(projectId, userId);
        return userRights.stream()
                .map(ProjectUserRight::getRight)
                .collect(Collectors.toSet());
    }
    
    /**
     * Check if a user has a specific right in a project
     */
    public boolean hasProjectRight(Long projectId, Long userId, ProjectRight right) {
        try {
            return projectUserRightRepository.existsByProjectIdAndUserIdAndRight(projectId, userId, right);
        } catch (Exception e) {
            System.err.println("Error checking project right: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove a user from a project
     */
    @Transactional
    public void removeUserFromProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Cannot remove project owner
        if (project.getOwner().equals(user)) {
            throw new RuntimeException("Cannot remove project owner");
        }
        
        project.removeParticipant(user);
        projectRepository.save(project);
        
        // Rights will be automatically removed by the cascade
    }


    /**
     * Grant all rights to a project owner
     * @param projectId the project ID
     */
    @Transactional
    public void grantAllRightsToOwner(Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
            
            User owner = project.getOwner();
            if (owner == null) {
                throw new RuntimeException("Project has no owner");
            }
            
            System.out.println("Granting all rights to owner: " + owner.getUsername() + " for project ID: " + projectId);
            
            // Grant all rights to owner
            for (ProjectRight right : ProjectRight.values()) {
                try {
                    System.out.println("Attempting to grant right: " + right.name() + " to owner");
                    // Check if right already exists
                    if (projectUserRightRepository.findByProjectAndUserAndRight(project, owner, right).isEmpty()) {
                        project.addUserRight(owner, right);
                    }
                } catch (Exception e) {
                    System.err.println("Error granting specific right " + right.name() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            projectRepository.save(project);
        } catch (Exception e) {
            System.err.println("Error in grantAllRightsToOwner: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Получает права пользователя на всех его проектах в виде Map, где ключ - ID проекта,
     * а значение - список прав пользователя на этом проекте
     * 
     * @param userId ID пользователя
     * @return Map с правами пользователя на всех проектах
     */
    public Map<Long, Set<ProjectRight>> getUserRightsForAllProjects(Long userId) {

        List<Object[]> result = projectRepository.findProjectIdAndRightNameByOwnerIdOrParticipantId(userId);
        Long currentProjectId = null;
        Map<Long, Set<ProjectRight>> userRights = new HashMap<>();
        for (Object[] row : result) {
            Long projectId = (Long) row[0];
            String rightName = (String) row[1];
            if (currentProjectId != null && projectId.equals(currentProjectId)) {
                userRights.get(projectId).add(ProjectRight.valueOf(rightName));
                continue;
            }
            currentProjectId = projectId;
            HashSet<ProjectRight> projectRights = new HashSet<>();
            projectRights.add(ProjectRight.valueOf(rightName));
            userRights.put(projectId, projectRights);

        }
        return userRights;
    }
} 