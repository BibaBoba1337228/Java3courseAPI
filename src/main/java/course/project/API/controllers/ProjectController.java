package course.project.API.controllers;

import course.project.API.dto.project.ProjectDTO;
import course.project.API.services.ProjectService;
import course.project.API.services.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.security.Principal;
import course.project.API.repositories.UserRepository;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @Autowired
    public ProjectController(ProjectService projectService, 
                           UserRepository userRepository,
                           PermissionService permissionService) {
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<ProjectDTO> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id, Principal principal) {
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasProjectAccess(user.getId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return projectService.getProjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my")
    public List<ProjectDTO> getMyProjects(Principal principal) {
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        return projectService.getMyProjects(user.getId());
    }

    @PostMapping
    public ProjectDTO createProject(@RequestBody ProjectDTO projectDTO, Principal principal) {
        return projectService.createProject(projectDTO, principal.getName());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectDTO projectDTO,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageProject(user.getId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return projectService.updateProject(id, projectDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Principal principal) {
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageProject(user.getId(), id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/participants/{userId}")
    public ResponseEntity<Void> addParticipantToProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Principal principal) {
            
        var currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageUserInProject(currentUser.getId(), projectId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        projectService.addParticipant(projectId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipantFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Principal principal) {
            
        var currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageUserInProject(currentUser.getId(), projectId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        projectService.removeParticipant(projectId, userId);
        return ResponseEntity.ok().build();
    }
} 