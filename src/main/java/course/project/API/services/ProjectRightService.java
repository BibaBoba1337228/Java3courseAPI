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
     * Add a user to a project with no rights initially
     */
    @Transactional
    public void addUserToProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Add user as participant
        project.addParticipant(user);
        
        // By default, give VIEW_PROJECT right
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
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        return project.hasRight(user, right);
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
} 