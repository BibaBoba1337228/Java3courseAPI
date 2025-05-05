package course.project.API.controllers;

import course.project.API.dto.project.ProjectDTO;
import course.project.API.dto.project.ProjectResponse;
import course.project.API.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public ProjectController(ProjectService projectService, UserRepository userRepository) {
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ProjectDTO> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my")
    public List<ProjectResponse> getMyProjects(Principal principal) {
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        return projectService.getMyProjectsWithUsers(user.getId());
    }

    @PostMapping
    public ProjectDTO createProject(@RequestBody ProjectDTO projectDTO, Principal principal) {
        return projectService.createProject(projectDTO, principal.getName());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable Long id, @RequestBody ProjectDTO projectDTO) {
        return projectService.updateProject(id, projectDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{projectId}/participants/{username}")
    public ResponseEntity<ProjectDTO> addParticipant(
            @PathVariable Long projectId,
            @PathVariable String username) {
        return projectService.addParticipantByUsername(projectId, username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{projectId}/participants/{username}")
    public ResponseEntity<ProjectDTO> removeParticipant(
            @PathVariable Long projectId,
            @PathVariable String username) {
        return projectService.removeParticipantByUsername(projectId, username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
} 