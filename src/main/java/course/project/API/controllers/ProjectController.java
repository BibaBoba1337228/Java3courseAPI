package course.project.API.controllers;

import course.project.API.dto.project.ProjectDTO;
import course.project.API.dto.project.ProjectResponse;
import course.project.API.models.ProjectRight;
import course.project.API.models.User;
import course.project.API.repositories.UserRepository;
import course.project.API.services.BoardService;
import course.project.API.services.ProjectRightService;
import course.project.API.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
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

    @GetMapping
    public List<ProjectDTO> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Get project first
            Optional<ProjectDTO> projectOpt = projectService.getProjectById(id);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ProjectDTO project = projectOpt.get();
            
            // If user is owner or participant, return the project
            boolean isOwner = project.getOwnerId() != null && project.getOwnerId().equals(currentUser.getId());
            boolean isParticipant = project.getParticipants() != null && 
                                   project.getParticipants().contains(currentUser.getUsername());
            
            if (isOwner || isParticipant) {
                return ResponseEntity.ok(project);
            } 
            
            // If user is neither owner nor participant and doesn't have VIEW_PROJECT right
            if (!projectRightService.hasProjectRight(id, currentUser.getId(), ProjectRight.VIEW_PROJECT)) {
                return ResponseEntity.status(403).body(null);
            }
            
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            System.err.println("Error in getProjectById: " + e.getMessage());
            return ResponseEntity.status(400).body(null);
        }
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
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectDTO projectDTO,
            @AuthenticationPrincipal User currentUser) {
        
        // Project owner can always update
        if (projectService.isProjectOwner(id, currentUser.getId())) {
            return projectService.updateProject(id, projectDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        }
        
        // Check if user has EDIT_PROJECT right
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
        
        // Only project owner can delete it
        if (!projectService.isProjectOwner(id, currentUser.getId())) {
            return ResponseEntity.status(403).body(null);
        }
        
        projectService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{projectId}/participants/{username}")
    public ResponseEntity<?> addParticipant(
            @PathVariable Long projectId,
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if user has MANAGE_MEMBERS right
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body("You don't have permission to add users to this project");
        }
        
        return projectService.addParticipantByUsernameWithResponse(projectId, username)
                .map(project -> {
                    // Создаем список с одним проектом для соответствия запрошенному формату ответа
                    List<ProjectResponse> projectList = List.of(project);
                    return ResponseEntity.ok(projectList);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{projectId}/participants/{username}")
    public ResponseEntity<ProjectDTO> removeParticipant(
            @PathVariable Long projectId,
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if user has MANAGE_MEMBERS right
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        return projectService.removeParticipantByUsername(projectId, username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{projectId}/participants/check")
    public ResponseEntity<Boolean> checkUserMembership(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Get project first
            Optional<ProjectDTO> projectOpt = projectService.getProjectById(projectId);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ProjectDTO project = projectOpt.get();
            
            // Check if user is owner or participant
            boolean isOwner = project.getOwnerId() != null && project.getOwnerId().equals(currentUser.getId());
            boolean isParticipant = project.getParticipants() != null && 
                                   project.getParticipants().contains(currentUser.getUsername());
            
            boolean isMember = isOwner || isParticipant;
            
            return ResponseEntity.ok(isMember);
        } catch (Exception e) {
            System.err.println("Error in checkUserMembership: " + e.getMessage());
            return ResponseEntity.status(400).body(false);
        }
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
    
    /**
     * Добавляет пользователя на все доски проекта по имени пользователя
     * и устанавливает маркер ACCESS_ALL_BOARDS для автоматического добавления
     * на все новые доски
     */
    @PostMapping("/{projectId}/boards/add-user/username/{username}")
    public ResponseEntity<Map<String, Object>> addUserToAllProjectBoardsByUsername(
            @PathVariable Long projectId,
            @PathVariable String username,
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
            int boardsCount = boardService.addUserToAllProjectBoardsByUsername(projectId, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("boardsCount", boardsCount);
            response.put("message", "Пользователь " + username + " добавлен на " + boardsCount + 
                " досок проекта и будет автоматически добавляться на все новые доски");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Удаляет пользователя со всех досок проекта по имени пользователя
     * и снимает маркер ACCESS_ALL_BOARDS - пользователь больше не будет
     * автоматически добавляться на новые доски
     */
    @DeleteMapping("/{projectId}/boards/remove-user/username/{username}")
    public ResponseEntity<Map<String, Object>> removeUserFromAllProjectBoardsByUsername(
            @PathVariable Long projectId,
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser) {
        
        // Проверяем права текущего пользователя
        boolean isOwner = projectService.isProjectOwner(projectId, currentUser.getId());
        boolean hasManageAccess = projectRightService.hasProjectRight(
            projectId, currentUser.getId(), ProjectRight.MANAGE_ACCESS);
            
        if (!isOwner && !hasManageAccess) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "У вас нет прав на удаление пользователей с досок проекта"
            ));
        }
        
        try {
            // Удаляем пользователя со всех досок
            int boardsCount = boardService.removeUserFromAllProjectBoardsByUsername(projectId, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("boardsCount", boardsCount);
            response.put("message", "Пользователь " + username + " удален с " + boardsCount + 
                " досок проекта и больше не будет автоматически добавляться на новые доски");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
} 