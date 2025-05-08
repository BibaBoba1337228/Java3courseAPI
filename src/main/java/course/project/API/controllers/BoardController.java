package course.project.API.controllers;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.board.BoardDTO;
import course.project.API.dto.board.BoardWithColumnsDTO;
import course.project.API.dto.project.ProjectDTO;
import course.project.API.models.BoardRight;
import course.project.API.models.DashBoardColumn;
import course.project.API.models.ProjectRight;
import course.project.API.models.User;
import course.project.API.services.BoardRightService;
import course.project.API.services.BoardService;
import course.project.API.services.ProjectRightService;
import course.project.API.services.ProjectService;
import course.project.API.services.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;
    private final BoardRightService boardRightService;
    private final ProjectRightService projectRightService;
    private final ProjectService projectService;
    private final WebSocketService webSocketService;

    @Autowired
    public BoardController(BoardService boardService, BoardRightService boardRightService, 
                         ProjectRightService projectRightService, ProjectService projectService,
                         WebSocketService webSocketService) {
        this.boardService = boardService;
        this.boardRightService = boardRightService;
        this.projectRightService = projectRightService;
        this.projectService = projectService;
        this.webSocketService = webSocketService;
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
            if (projectService.isProjectOwner(projectId, currentUser.getId())) {
                return ResponseEntity.ok(boardService.getBoardsByProjectId(projectId));
            }
            
            Optional<ProjectDTO> projectOpt = projectService.getProjectById(projectId);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ProjectDTO project = projectOpt.get();
            boolean isParticipant = project.getParticipants() != null && 
                                   project.getParticipants().contains(currentUser.getUsername());
            
            if (!isParticipant) {
                return ResponseEntity.status(403).body(null);
            }
            
            List<BoardDTO> allBoards = boardService.getBoardsByProjectId(projectId);
            List<BoardDTO> userBoards = new ArrayList<>();
            
            for (BoardDTO board : allBoards) {
                if (board.getId() != null && boardRightService.hasBoardRight(board.getId(), currentUser.getId(), BoardRight.VIEW_BOARD)) {
                    userBoards.add(board);
                }
            }
            
            return ResponseEntity.ok(userBoards);
        } catch (Exception e) {
            System.err.println("Error in getBoardsByProject: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

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
        
        if (projectService.isProjectOwner(boardDTO.getProjectId(), currentUser.getId())) {
            return boardService.createBoard(boardDTO)
                    .map(board -> ResponseEntity.status(HttpStatus.CREATED).body(board))
                    .orElse(ResponseEntity.badRequest().build());
        }
        
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
            boardDTO.setId(boardId);
            
            if (boardDTO.getTags() != null) {
                boardDTO.getTags().removeIf(tag -> tag == null);
                for (var tag : boardDTO.getTags()) {
                    if (tag.getBoardId() == null) {
                        tag.setBoardId(boardId);
                    }
                }
            }
            
            if (boardDTO.getParticipantIds() != null) {
                boardDTO.getParticipantIds().removeIf(id -> id == null);
            }
        
        Long projectId = boardService.getBoardById(boardId)
                .map(BoardDTO::getProjectId)
                .orElse(null);
        
        if (projectId == null) {
                return ResponseEntity.status(404).body(null);
        }
        
        if (projectService.isProjectOwner(projectId, currentUser.getId())) {
                return boardService.updateBoard(boardId, boardDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(404).build());
        }
        
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
        
        Long projectId = boardService.getBoardById(boardId)
                .map(BoardDTO::getProjectId)
                .orElse(null);
        
        if (projectId == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (projectService.isProjectOwner(projectId, currentUser.getId())) {
            boardService.deleteBoard(boardId);
            return ResponseEntity.noContent().build();
        }
        
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
        
        Long projectId = boardService.getBoardById(boardId)
                .map(BoardDTO::getProjectId)
                .orElse(null);
                
        if (projectId == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (projectService.isProjectOwner(projectId, currentUser.getId())) {
            boolean added = boardService.addParticipant(boardId, userId);
            if (added) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        }
        
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
        
        Long projectId = boardService.getBoardById(boardId)
                .map(BoardDTO::getProjectId)
                .orElse(null);
                
        if (projectId == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (projectService.isProjectOwner(projectId, currentUser.getId())) {
            boolean removed = boardService.removeParticipant(boardId, userId);
            if (removed) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        }
        
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

    @PostMapping("/{boardId}/columns")
    public ResponseEntity<DashBoardColumn> createColumn(
            @PathVariable Long boardId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User currentUser) {
        
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_COLUMNS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        String title = (String) payload.get("title");
        Integer position = (Integer) payload.get("position");
        
        if (title == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        DashBoardColumn column = boardService.createColumn(boardId, title, position);
        if (column != null) {
            Map<String, Object> notificationPayload = new HashMap<>(payload);
            notificationPayload.put("columnId", column.getId());
            notificationPayload.put("initiatedBy", currentUser.getUsername());
            webSocketService.sendMessageToBoard(boardId, "COLUMN_CREATED", notificationPayload);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(column);
        }
        
        return ResponseEntity.badRequest().body(null);
    }

    @PutMapping("/{boardId}/columns/{columnId}")
    public ResponseEntity<DashBoardColumn> updateColumn(
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User currentUser) {
        
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_COLUMNS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        String title = (String) payload.get("title");
        Integer position = (Integer) payload.get("position");
        
        if (title == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        DashBoardColumn column = boardService.updateColumn(boardId, columnId, title, position);
        if (column != null) {
            Map<String, Object> notificationPayload = new HashMap<>(payload);
            notificationPayload.put("columnId", columnId);
            notificationPayload.put("initiatedBy", currentUser.getUsername());
            webSocketService.sendMessageToBoard(boardId, "COLUMN_UPDATED", notificationPayload);
            
            return ResponseEntity.ok(column);
        }
        
        return ResponseEntity.badRequest().body(null);
    }

    @DeleteMapping("/{boardId}/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @AuthenticationPrincipal User currentUser) {
        
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_COLUMNS)) {
            return ResponseEntity.status(403).build();
        }
        
        boolean deleted = boardService.deleteColumn(boardId, columnId);
        if (deleted) {
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("columnId", columnId);
            notificationPayload.put("initiatedBy", currentUser.getUsername());
            webSocketService.sendMessageToBoard(boardId, "COLUMN_DELETED", notificationPayload);
            
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{boardId}/columns/reorder")
    public ResponseEntity<SimpleDTO> reorderColumns(
            @PathVariable Long boardId,
            @RequestBody Map<String, List<Map<String, Object>>> payload,
            @AuthenticationPrincipal User currentUser) {
        
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_COLUMNS)) {
            return ResponseEntity.status(403).build();
        }
        
        List<Map<String, Object>> columns = payload.get("columns");
        if (columns == null || columns.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean reordered = boardService.reorderColumns(boardId, columns);
        if (reordered) {
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("columns", columns);
            notificationPayload.put("initiatedBy", currentUser.getUsername());
            webSocketService.sendMessageToBoard(boardId, "COLUMNS_REORDERED", notificationPayload);
            
            return ResponseEntity.ok(new SimpleDTO("Columns reordered successfully"));
        }
        
        return ResponseEntity.badRequest().build();
    }
} 