package course.project.API.controllers;

import course.project.API.dto.RightDto;
import course.project.API.models.ProjectRight;
import course.project.API.services.ProjectRightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import course.project.API.models.User;

import java.util.Set;

@RestController
@RequestMapping("/api/projects/{projectId}/rights")
public class ProjectRightController {

    private final ProjectRightService projectRightService;

    @Autowired
    public ProjectRightController(ProjectRightService projectRightService) {
        this.projectRightService = projectRightService;
    }

    @PostMapping("/users")
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

    @GetMapping("/users/{userId}")
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

    @PostMapping("/grant")
    public ResponseEntity<?> grantRight(
            @PathVariable Long projectId,
            @RequestBody RightDto rightDto,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_RIGHTS right
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_RIGHTS)) {
            return ResponseEntity.status(403).body("You don't have permission to manage rights in this project");
        }
        
        try {
            ProjectRight right = ProjectRight.valueOf(rightDto.getRightName());
            projectRightService.grantProjectRight(projectId, rightDto.getUserId(), right);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid right name");
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revokeRight(
            @PathVariable Long projectId,
            @RequestBody RightDto rightDto,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_RIGHTS right
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_RIGHTS)) {
            return ResponseEntity.status(403).body("You don't have permission to manage rights in this project");
        }
        
        try {
            ProjectRight right = ProjectRight.valueOf(rightDto.getRightName());
            projectRightService.revokeProjectRight(projectId, rightDto.getUserId(), right);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid right name");
        }
    }

    @DeleteMapping("/users/{userId}")
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