package course.project.API.controllers;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.project.ProjectDTO;
import course.project.API.dto.project.ProjectWithParticipantsOwnerDTO;
import course.project.API.dto.project.ProjectWithParticipantsOwnerInvitationsDTO;
import course.project.API.models.ProjectRight;
import course.project.API.models.User;
import course.project.API.models.InvitationStatus;
import course.project.API.repositories.UserRepository;
import course.project.API.services.BoardService;
import course.project.API.services.ProjectRightService;
import course.project.API.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final ProjectRightService projectRightService;
    private final BoardService boardService;

    @Autowired
    public ProjectController(ProjectService projectService, UserRepository userRepository, 
                           ProjectRightService projectRightService, BoardService boardService) {
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.projectRightService = projectRightService;
        this.boardService = boardService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectWithParticipantsOwnerInvitationsDTO> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        try {
            logger.info("Getting project with ID: {}", id);
            Optional<ProjectWithParticipantsOwnerInvitationsDTO> projectOpt = projectService.getProjectWithParticipantsOwnerInvitationsById(id);
            if (projectOpt.isEmpty()) {
                logger.warn("Project not found for ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            if (!projectRightService.hasProjectRight(id, currentUser.getId(), ProjectRight.VIEW_PROJECT)) {
                logger.warn("User {} doesn't have rights to view project: {}", currentUser.getUsername(), id);
                return ResponseEntity.status(403).body(null);
            }

            ProjectWithParticipantsOwnerInvitationsDTO project = projectOpt.get();
            if (!project.getInvitations().isEmpty()) {
                logger.info("First participant is a {}", project.getInvitations().iterator().next().getClass().getName());
            }
            
            // Filter invitations to only include those with PENDING status
            project.setInvitations(project.getInvitations().stream()
                .filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
                .collect(Collectors.toSet()));
            
            logger.info("Returning project DTO: {}, ID: {}", project.getClass().getName(), project.getId());

            return ResponseEntity.ok(project);
        } catch (Exception e) {
            logger.error("Error in getProjectWithUsersById: {}", e.getMessage(), e);
            return ResponseEntity.status(400).body(null);
        }
    }


    @GetMapping("/my")
    public List<ProjectWithParticipantsOwnerDTO> getMyProjects(@AuthenticationPrincipal User currentUser) {
        return projectService.getMyProjectsWithUsers(currentUser);
    }

    @PostMapping
    public ProjectDTO createProject(@RequestBody ProjectDTO projectDTO, Principal principal) {
        return projectService.createProject(projectDTO, principal.getName());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectDTO projectDTO,
            @AuthenticationPrincipal User currentUser) {
        

        
        if (!projectRightService.hasProjectRight(id, currentUser.getId(), ProjectRight.EDIT_PROJECT)) {
            return ResponseEntity.status(403).body(null);
        }
        
        return projectService.updateProject(id, projectDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        if (!projectService.isProjectOwner(id, currentUser.getId())) {
            return ResponseEntity.status(403).body(null);
        }
        
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{projectId}/participants/{id}")
    public ResponseEntity<?> addParticipant(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body("You don't have permission to add users to this project");
        }
        
        if (projectService.addParticipant(projectId, id)) {
            return ResponseEntity.ok().body(new SimpleDTO("Пользователь успешно добавлен"));
        }
        
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{projectId}/participants/{id}")
    public ResponseEntity<SimpleDTO> removeParticipant(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body(null);
        }
        if (projectService.removeParticipant(projectId, id)){
            return ResponseEntity.ok().body(new SimpleDTO("Удаление прошло успешно"));
        }

        return ResponseEntity.badRequest().build();
    }



    @PostMapping("/{projectId}/boards/add-user/{userId}")
    public ResponseEntity<Map<String, Object>> addUserToAllProjectBoards(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Проверяем права текущего пользователя
        boolean isOwner = projectService.isProjectOwner(projectId, currentUser.getId());
        boolean hasManageAccess = projectRightService.hasProjectRight(
            projectId, currentUser.getId(), ProjectRight.MANAGE_ACCESS);
            
        if (!isOwner && !hasManageAccess) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "У вас нет прав на добавление пользователей на доски проекта"
            ));
        }
        
        try {
            // Добавляем пользователя на все доски
            int boardsCount = boardService.addUserToAllProjectBoards(projectId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("boardsCount", boardsCount);
            response.put("message", "Пользователь добавлен на " + boardsCount + " досок проекта");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    

    

} 