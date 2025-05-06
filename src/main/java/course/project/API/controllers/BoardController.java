package course.project.API.controllers;

import course.project.API.dto.board.BoardDTO;
import course.project.API.dto.board.BoardWithColumnsDTO;
import course.project.API.dto.project.ProjectDTO;
import course.project.API.models.BoardRight;
import course.project.API.models.ProjectRight;
import course.project.API.models.User;
import course.project.API.services.BoardRightService;
import course.project.API.services.BoardService;
import course.project.API.services.ProjectRightService;
import course.project.API.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;
    private final BoardRightService boardRightService;
    private final ProjectRightService projectRightService;
    private final ProjectService projectService;

    @Autowired
    public BoardController(BoardService boardService, BoardRightService boardRightService, 
                         ProjectRightService projectRightService, ProjectService projectService) {
        this.boardService = boardService;
        this.boardRightService = boardRightService;
        this.projectRightService = projectRightService;
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<BoardDTO>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<BoardDTO>> getBoardsByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // First check if user is project owner (special case)
            if (projectService.isProjectOwner(projectId, currentUser.getId())) {
                return ResponseEntity.ok(boardService.getBoardsByProjectId(projectId));
            }
            
            // Check if user is project participant
            Optional<ProjectDTO> projectOpt = projectService.getProjectById(projectId);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ProjectDTO project = projectOpt.get();
            boolean isParticipant = project.getParticipants() != null && 
                                   project.getParticipants().contains(currentUser.getUsername());
            
            // If user is not a participant, return 403
            if (!isParticipant) {
                return ResponseEntity.status(403).body(null);
            }
            
            // For regular participants, only return boards where they are a participant
            List<BoardDTO> allBoards = boardService.getBoardsByProjectId(projectId);
            List<BoardDTO> userBoards = new ArrayList<>();
            
            for (BoardDTO board : allBoards) {
                // Check if user has rights to this specific board
                if (board.getId() != null && boardRightService.hasBoardRight(board.getId(), currentUser.getId(), BoardRight.VIEW_BOARD)) {
                    userBoards.add(board);
                }
            }
            
            return ResponseEntity.ok(userBoards);
        } catch (Exception e) {
            // Log the error
            System.err.println("Error in getBoardsByProject: " + e.getMessage());
            // Return empty list as a fallback
            return ResponseEntity.ok(List.of());
        }
    }

    // Вспомогательный класс для ошибок
    class ErrorResponse {
        private String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<?> getBoardById(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User currentUser) {
        try {
            Optional<BoardDTO> boardOpt = boardService.getBoardById(boardId);
            if (boardOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new ErrorResponse("Board not found"));
            }
            Long projectId = boardOpt.get().getProjectId();
            if (projectId == null) {
                return ResponseEntity.status(400).body(new ErrorResponse("Project ID is null"));
            }
            if (projectService.isProjectOwner(projectId, currentUser.getId())) {
                Optional<BoardWithColumnsDTO> details = boardService.getBoardWithDetails(boardId);
                if (details.isPresent()) {
                    return ResponseEntity.ok(details.get());
                } else {
                    return ResponseEntity.status(404).body(new ErrorResponse("Board not found"));
                }
            }
            Optional<ProjectDTO> projectOpt = projectService.getProjectById(projectId);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new ErrorResponse("Project not found"));
            }
            ProjectDTO project = projectOpt.get();
            boolean isProjectParticipant = project.getParticipants() != null && 
                project.getParticipants().contains(currentUser.getUsername());
            if (isProjectParticipant && !boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD)) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied: no board rights"));
            } else if (!isProjectParticipant) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied: not a project participant"));
            }
            return ResponseEntity.ok(boardService.getBoardWithDetails(boardId));
        } catch (Exception e) {
            System.err.println("Error in getBoardById: " + e.getMessage());
            return ResponseEntity.status(400).body(new ErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<BoardDTO> createBoard(
            @RequestBody BoardDTO boardDTO,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if user is project owner - automatic permission
        if (projectService.isProjectOwner(boardDTO.getProjectId(), currentUser.getId())) {
            return boardService.createBoard(boardDTO)
                    .map(board -> ResponseEntity.status(HttpStatus.CREATED).body(board))
                    .orElse(ResponseEntity.badRequest().build());
        }
        
        // Check if user has CREATE_BOARDS right on the project
        if (!projectRightService.hasProjectRight(boardDTO.getProjectId(), currentUser.getId(), ProjectRight.CREATE_BOARDS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        return boardService.createBoard(boardDTO)
                .map(board -> ResponseEntity.status(HttpStatus.CREATED).body(board))
                .orElse(ResponseEntity.badRequest().build());
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<BoardDTO> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardDTO boardDTO,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Устанавливаем ID доски в DTO, чтобы избежать ошибки "The given id must not be null"
            boardDTO.setId(boardId);
            
            // Проверяем теги на null в boardDTO и устанавливаем null-безопасные значения
            if (boardDTO.getTags() != null) {
                boardDTO.getTags().removeIf(tag -> tag == null);
                for (var tag : boardDTO.getTags()) {
                    if (tag.getBoardId() == null) {
                        tag.setBoardId(boardId);
                    }
                }
            }
            
            // Проверяем participantIds на null в boardDTO
            if (boardDTO.getParticipantIds() != null) {
                boardDTO.getParticipantIds().removeIf(id -> id == null);
            }
            
            // Get project ID
            Long projectId = boardService.getBoardById(boardId)
                    .map(BoardDTO::getProjectId)
                    .orElse(null);
            
            if (projectId == null) {
                return ResponseEntity.status(404).body(null);
            }
            
            // Check if user is project owner - automatic permission
            if (projectService.isProjectOwner(projectId, currentUser.getId())) {
                return boardService.updateBoard(boardId, boardDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).build());
            }
            
            // Check if user has EDIT_BOARDS right on the project
            if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.EDIT_BOARDS)) {
                return ResponseEntity.status(403).body(null);
            }
            
            return boardService.updateBoard(boardId, boardDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
                
        } catch (Exception e) {
            e.printStackTrace(); // Добавляем подробное логирование ошибки
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User currentUser) {
        
        // Get project ID
        Long projectId = boardService.getBoardById(boardId)
                .map(BoardDTO::getProjectId)
                .orElse(null);
        
        if (projectId == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if user is project owner - automatic permission
        if (projectService.isProjectOwner(projectId, currentUser.getId())) {
            boardService.deleteBoard(boardId);
            return ResponseEntity.noContent().build();
        }
        
        // Check if user has DELETE_BOARDS right on the project
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.DELETE_BOARDS)) {
            return ResponseEntity.status(403).build();
        }
        
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{boardId}/participants/{userId}")
    public ResponseEntity<Void> addParticipant(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Get project ID
        Long projectId = boardService.getBoardById(boardId)
                .map(BoardDTO::getProjectId)
                .orElse(null);
                
        if (projectId == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if user is project owner - automatic permission
        if (projectService.isProjectOwner(projectId, currentUser.getId())) {
            boolean added = boardService.addParticipant(boardId, userId);
            if (added) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        }
        
        // Check if user has MANAGE_MEMBERS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).build();
        }
        
        boolean added = boardService.addParticipant(boardId, userId);
        if (added) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{boardId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Get project ID
        Long projectId = boardService.getBoardById(boardId)
                .map(BoardDTO::getProjectId)
                .orElse(null);
                
        if (projectId == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if user is project owner - automatic permission
        if (projectService.isProjectOwner(projectId, currentUser.getId())) {
            boolean removed = boardService.removeParticipant(boardId, userId);
            if (removed) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        }
        
        // Check if user has MANAGE_MEMBERS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).build();
        }
        
        boolean removed = boardService.removeParticipant(boardId, userId);
        if (removed) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 