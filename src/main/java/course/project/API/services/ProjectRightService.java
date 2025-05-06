package course.project.API.services;

import course.project.API.models.Project;
import course.project.API.models.ProjectRight;
import course.project.API.models.ProjectUserRight;
import course.project.API.models.User;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.ProjectUserRightRepository;
import course.project.API.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectRightService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectUserRightRepository projectUserRightRepository;

    @Autowired
    public ProjectRightService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ProjectUserRightRepository projectUserRightRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectUserRightRepository = projectUserRightRepository;
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
    }
    
    /**
     * Get all rights for a user in a project
     */
    public Set<ProjectRight> getUserProjectRights(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Project owner has all rights
        if (project.getOwner().equals(user)) {
            return new HashSet<>(Arrays.asList(ProjectRight.values()));
        }
        
        // Get user's rights
        List<ProjectUserRight> userRights = projectUserRightRepository.findByProjectAndUser(project, user);
        return userRights.stream()
                .map(ProjectUserRight::getRight)
                .collect(Collectors.toSet());
    }
    
    /**
     * Check if a user has a specific right in a project
     */
    public boolean hasProjectRight(Long projectId, Long userId, ProjectRight right) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            // Project owner has all rights
            if (project.getOwner().equals(user)) {
                return true;
            }
            
            // Project participant checking for VIEW_PROJECT right
            if (right == ProjectRight.VIEW_PROJECT && project.getParticipants().contains(user)) {
                return true;
            }
            
            return project.hasRight(user, right);
        } catch (Exception e) {
            // Log error and gracefully handle it
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
     * Grant a specific right to a user in a project by username
     */
    @Transactional
    public void grantProjectRightByUsername(Long projectId, String username, ProjectRight right) {
        try {
            if (projectId == null) {
                throw new IllegalArgumentException("Project ID must not be null");
            }
            
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username must not be null or empty");
            }
            
            if (right == null) {
                throw new IllegalArgumentException("Right must not be null");
            }
            
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
            
            // Print debug info
            System.out.println("Granting right: " + right + " to user: " + username + " (ID: " + user.getId() + ") for project: " + projectId);
            
            // Check if user is participant
            if (!project.getParticipants().contains(user)) {
                project.addParticipant(user);
            }
            
            // Check if right already exists
            if (projectUserRightRepository.findByProjectAndUserAndRight(project, user, right).isEmpty()) {
                project.addUserRight(user, right);
                projectRepository.save(project);
            }
        } catch (Exception e) {
            System.err.println("Error in grantProjectRightByUsername: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Revoke a specific right from a user in a project by username
     */
    @Transactional
    public void revokeProjectRightByUsername(Long projectId, String username, ProjectRight right) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        // Cannot revoke rights from project owner
        if (project.getOwner().equals(user)) {
            throw new RuntimeException("Cannot revoke rights from project owner");
        }
        
        project.removeUserRight(user, right);
        projectRepository.save(project);
    }
    
    /**
     * Get all rights for a user in a project by username
     */
    public Set<ProjectRight> getUserProjectRightsByUsername(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        // Project owner has all rights
        if (project.getOwner().equals(user)) {
            return new HashSet<>(Arrays.asList(ProjectRight.values()));
        }
        
        // Get user's rights
        List<ProjectUserRight> userRights = projectUserRightRepository.findByProjectAndUser(project, user);
        return userRights.stream()
                .map(ProjectUserRight::getRight)
                .collect(Collectors.toSet());
    }
    
    /**
     * Check if a user has a specific right in a project by username
     */
    public boolean hasProjectRightByUsername(Long projectId, String username, ProjectRight right) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
            
            // Project owner has all rights
            if (project.getOwner() != null && project.getOwner().getUsername().equals(username)) {
                return true;
            }
            
            // Project participant checking for VIEW_PROJECT right
            if (right == ProjectRight.VIEW_PROJECT && project.getParticipants().contains(user)) {
                return true;
            }
            
            return project.hasRight(user, right);
        } catch (Exception e) {
            // Log error and gracefully handle it
            System.err.println("Error checking project right: " + e.getMessage());
            return false;
        }
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
} 