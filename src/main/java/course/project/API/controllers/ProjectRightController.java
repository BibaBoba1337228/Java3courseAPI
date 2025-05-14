package course.project.API.controllers;

import course.project.API.dto.ProjectRightsDTO;
import course.project.API.dto.RightDto;
import course.project.API.models.ProjectRight;
import course.project.API.services.ProjectRightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import course.project.API.models.User;
import course.project.API.repositories.ProjectRepository;

import java.util.Set;

@RestController
public class ProjectRightController {

    private final ProjectRightService projectRightService;
    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectRightController(ProjectRightService projectRightService, ProjectRepository projectRepository) {
        this.projectRightService = projectRightService;
        this.projectRepository = projectRepository;
    }

    /**
     * Получает все права пользователя на всех проектах одним запросом
     */
    @GetMapping("/api/user-project-rights/{userId}")
    public ResponseEntity<ProjectRightsDTO> getUserRightsForAllProjects(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Разрешаем только запрашивать свои собственные права
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body(null);
        }
        
        var rights = projectRightService.getUserRightsForAllProjects(userId);
        ProjectRightsDTO result = new ProjectRightsDTO(rights);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/projects/{projectId}/rights/users")
    public ResponseEntity<?> addUserToProject(
            @PathVariable Long projectId,
            @RequestBody Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_MEMBERS right
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body("You don't have permission to add users to this project");
        }
        
        projectRightService.addUserToProject(projectId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/projects/{projectId}/rights/users/{userId}")
    public ResponseEntity<Set<ProjectRight>> getUserRights(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has VIEW_PROJECT right or is requesting their own rights
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.VIEW_PROJECT) 
                && !currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body(null);
        }
        
        Set<ProjectRight> rights = projectRightService.getUserProjectRights(projectId, userId);
        return ResponseEntity.ok(rights);
    }
    


    @PostMapping("/api/projects/{projectId}/rights/grant")
    public ResponseEntity<?> grantRight(
            @PathVariable Long projectId,
            @RequestBody RightDto rightDto,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Validation - check required parameters
            if (projectId == null) {
                return ResponseEntity.badRequest().body("Project ID must not be null");
            }
            
            if (rightDto == null) {
                return ResponseEntity.badRequest().body("Request body cannot be null");
            }
            
            if (rightDto.getRightName() == null || rightDto.getRightName().isEmpty()) {
                return ResponseEntity.badRequest().body("Right name must be provided");
            }
            
            if ((rightDto.getUsername() == null || rightDto.getUsername().isEmpty()) 
                    && rightDto.getUserId() == null) {
                return ResponseEntity.badRequest().body("Either username or userId must be provided");
            }
            
            // Check if current user has MANAGE_RIGHTS right
            if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_RIGHTS)) {
                return ResponseEntity.status(403).body("You don't have permission to manage rights in this project");
            }
            
            // Check if trying to modify project owner's rights
            boolean isTargetUserOwner = false;
            if (rightDto.getUsername() != null && !rightDto.getUsername().isEmpty()) {
                isTargetUserOwner = projectRepository.findById(projectId)
                    .map(project -> project.getOwner().getUsername().equals(rightDto.getUsername()))
                    .orElse(false);
            } else if (rightDto.getUserId() != null) {
                isTargetUserOwner = projectRepository.findById(projectId)
                    .map(project -> project.getOwner().getId().equals(rightDto.getUserId()))
                    .orElse(false);
            }
            
            if (isTargetUserOwner) {
                return ResponseEntity.badRequest().body("Cannot modify project owner's rights. Project owners always have all rights.");
            }
            
            try {
                ProjectRight right = ProjectRight.valueOf(rightDto.getRightName());
                
                if (rightDto.getUserId() != null) {
                    projectRightService.grantProjectRight(projectId, rightDto.getUserId(), right);
                }
                
                return ResponseEntity.ok().build();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid right name: " + rightDto.getRightName());
            }
        } catch (Exception e) {
            // Log the full error for debugging
            System.err.println("Error in grantRight: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error granting right: " + e.getMessage());
        }
    }

    @PostMapping("/api/projects/{projectId}/rights/revoke")
    public ResponseEntity<?> revokeRight(
            @PathVariable Long projectId,
            @RequestBody RightDto rightDto,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Validation - check required parameters
            if (projectId == null) {
                return ResponseEntity.badRequest().body("Project ID must not be null");
            }
            
            if (rightDto == null) {
                return ResponseEntity.badRequest().body("Request body cannot be null");
            }
            
            if (rightDto.getRightName() == null || rightDto.getRightName().isEmpty()) {
                return ResponseEntity.badRequest().body("Right name must be provided");
            }
            
            if ((rightDto.getUsername() == null || rightDto.getUsername().isEmpty()) 
                    && rightDto.getUserId() == null) {
                return ResponseEntity.badRequest().body("Either username or userId must be provided");
            }
            
            // Check if current user has MANAGE_RIGHTS right
            if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_RIGHTS)) {
                return ResponseEntity.status(403).body("You don't have permission to manage rights in this project");
            }
            
            // Check if trying to modify project owner's rights
            boolean isTargetUserOwner = false;
            if (rightDto.getUsername() != null && !rightDto.getUsername().isEmpty()) {
                isTargetUserOwner = projectRepository.findById(projectId)
                    .map(project -> project.getOwner().getUsername().equals(rightDto.getUsername()))
                    .orElse(false);
            } else if (rightDto.getUserId() != null) {
                isTargetUserOwner = projectRepository.findById(projectId)
                    .map(project -> project.getOwner().getId().equals(rightDto.getUserId()))
                    .orElse(false);
            }
            
            if (isTargetUserOwner) {
                return ResponseEntity.badRequest().body("Cannot modify project owner's rights. Project owners always have all rights.");
            }
            
            try {
                ProjectRight right = ProjectRight.valueOf(rightDto.getRightName());
                
                if (rightDto.getUserId() != null) {
                    projectRightService.revokeProjectRight(projectId, rightDto.getUserId(), right);
                }
                
                return ResponseEntity.ok().build();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid right name: " + rightDto.getRightName());
            }
        } catch (Exception e) {
            // Log the full error for debugging
            System.err.println("Error in revokeRight: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error revoking right: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/projects/{projectId}/rights/users/{userId}")
    public ResponseEntity<?> removeUserFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_MEMBERS right
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body("You don't have permission to remove users from this project");
        }
        
        projectRightService.removeUserFromProject(projectId, userId);
        return ResponseEntity.ok().build();
    }
} 