package course.project.API.controllers;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.board.BoardDTO;
import course.project.API.dto.board.BoardWithColumnsDTO;
import course.project.API.dto.board.BoardWithParticipantsDTO;
import course.project.API.dto.project.ProjectDTO;
import course.project.API.dto.project.ProjectWithParticipantsOwnerInvitationsDTO;
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
import java.util.HashSet;
import java.util.Set;

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
    public ResponseEntity<List<BoardWithParticipantsDTO>> getBoardsByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) {
        
        try {

            List<BoardWithParticipantsDTO> allBoards = boardService.getBoardsByProjectId(projectId);
            List<BoardWithParticipantsDTO> userBoards = new ArrayList<>();
            
            for (BoardWithParticipantsDTO board : allBoards) {
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

    @GetMapping("/{boardId}")
    public ResponseEntity<?> getBoardById(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User currentUser) {
        try {
            if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD)) {
                return ResponseEntity.status(403).body(new SimpleDTO("Access denied: no board rights"));
            }

            BoardWithColumnsDTO board = boardService.getBoardWithDetails(boardId);

            return ResponseEntity.ok(board);
        } catch (Exception e) {
            System.err.println("Error in getBoardById: " + e.getMessage());
            return ResponseEntity.status(400).body(new SimpleDTO("Internal error: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<BoardDTO> createBoard(
            @RequestBody BoardDTO boardDTO,
            @AuthenticationPrincipal User currentUser) {
        
        if (!projectRightService.hasProjectRight(boardDTO.getProjectId(), currentUser.getId(), ProjectRight.CREATE_BOARDS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        if (boardDTO.getParticipantIds() == null) {
            boardDTO.setParticipantIds(new HashSet<>());
        }
        
        // Add the current user to the board participants
        boardDTO.getParticipantIds().add(currentUser.getId());
        
        return boardService.createBoard(boardDTO)
                .map(board -> {
                    // If socketEvent is true, send a notification about the board creation
                    webSocketService.notifyProjectParticipants(boardDTO.getProjectId(), "BOARD_CREATED", board);
                    return ResponseEntity.status(HttpStatus.CREATED).body(board);
                })
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

            
            // Проверяем право на редактирование досок в проекте
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

        // Проверяем право на удаление досок в проекте
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
        
        
        // Проверяем право на управление участниками доски
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
        
        System.out.println("Creating column in board " + boardId + " by user " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
        
        // Get all rights for debugging
        Set<BoardRight> userRights = boardRightService.getUserBoardRights(boardId, currentUser.getId());
        System.out.println("User rights on board: " + userRights);
        
        // Accept either CREATE_SECTIONS or MOVE_COLUMNS right
        boolean hasCreateSectionsRight = boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.CREATE_SECTIONS);
        boolean hasMoveColumnsRight = boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_COLUMNS);
        
        System.out.println("Has CREATE_SECTIONS right: " + hasCreateSectionsRight);
        System.out.println("Has MOVE_COLUMNS right: " + hasMoveColumnsRight);
        
        if (!hasCreateSectionsRight && !hasMoveColumnsRight) {
            System.out.println("Permission denied: User does not have CREATE_SECTIONS or MOVE_COLUMNS right");
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
        
        System.out.println("Updating column " + columnId + " in board " + boardId + " by user " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
        
        // Get all rights for debugging
        Set<BoardRight> userRights = boardRightService.getUserBoardRights(boardId, currentUser.getId());
        System.out.println("User rights on board: " + userRights);
        
        // Accept either EDIT_SECTIONS or MOVE_COLUMNS right for column updates
        boolean hasEditSectionsRight = boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.EDIT_SECTIONS);
        boolean hasMoveColumnsRight = boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_COLUMNS);
        
        System.out.println("Has EDIT_SECTIONS right: " + hasEditSectionsRight);
        System.out.println("Has MOVE_COLUMNS right: " + hasMoveColumnsRight);
        
        if (!hasEditSectionsRight && !hasMoveColumnsRight) {
            System.out.println("Permission denied: User does not have EDIT_SECTIONS or MOVE_COLUMNS right");
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
            
            return ResponseEntity.ok().build();
        }
        
        return ResponseEntity.badRequest().body(null);
    }

    @DeleteMapping("/{boardId}/columns/{columnId}")
    public ResponseEntity<?> deleteColumn(
            @PathVariable Long boardId,
            @PathVariable Long columnId,
            @AuthenticationPrincipal User currentUser) {
        
        System.out.println("Deleting column " + columnId + " in board " + boardId + " by user " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
        
        // Get all rights for debugging
        Set<BoardRight> userRights = boardRightService.getUserBoardRights(boardId, currentUser.getId());
        System.out.println("User rights on board: " + userRights);
        
        // Accept either DELETE_SECTIONS or MOVE_COLUMNS right
        boolean hasDeleteSectionsRight = boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.DELETE_SECTIONS);
        boolean hasMoveColumnsRight = boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_COLUMNS);
        
        System.out.println("Has DELETE_SECTIONS right: " + hasDeleteSectionsRight);
        System.out.println("Has MOVE_COLUMNS right: " + hasMoveColumnsRight);
        
        if (!hasDeleteSectionsRight && !hasMoveColumnsRight) {
            System.out.println("Permission denied: User does not have DELETE_SECTIONS or MOVE_COLUMNS right");
            return ResponseEntity.status(403).build();
        }
        
        try {
            boolean deleted = boardService.deleteColumn(boardId, columnId);
            if (deleted) {
                Map<String, Object> notificationPayload = new HashMap<>();
                notificationPayload.put("columnId", columnId);
                notificationPayload.put("initiatedBy", currentUser.getUsername());
                webSocketService.sendMessageToBoard(boardId, "COLUMN_DELETED", notificationPayload);
                
                return ResponseEntity.noContent().build();
            }
            
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("'Done' column cannot be deleted")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
            }
            throw e;
        }
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

    @PostMapping("/{boardId}/debug/grant-edit-sections")
    public ResponseEntity<String> debugGrantEditSections(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Only allow in development environment
            boardRightService.grantBoardRight(boardId, currentUser.getId(), BoardRight.EDIT_SECTIONS);
            Set<BoardRight> rights = boardRightService.getUserBoardRights(boardId, currentUser.getId());
            
            return ResponseEntity.ok("Granted EDIT_SECTIONS right to user. Current rights: " + rights);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{boardId}/debug/rights")
    public ResponseEntity<Set<BoardRight>> debugGetRights(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            Set<BoardRight> rights = boardRightService.getUserBoardRights(boardId, currentUser.getId());
            System.out.println("User rights for board " + boardId + ": " + rights);
            return ResponseEntity.ok(rights);
        } catch (Exception e) {
            System.err.println("Error getting rights: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/{boardId}/debug/grant-column-rights")
    public ResponseEntity<String> debugGrantColumnRights(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Выдаем все права для работы с колонками
            boardRightService.grantBoardRight(boardId, currentUser.getId(), BoardRight.CREATE_SECTIONS);
            boardRightService.grantBoardRight(boardId, currentUser.getId(), BoardRight.EDIT_SECTIONS);
            boardRightService.grantBoardRight(boardId, currentUser.getId(), BoardRight.DELETE_SECTIONS);
            boardRightService.grantBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_COLUMNS);
            
            Set<BoardRight> rights = boardRightService.getUserBoardRights(boardId, currentUser.getId());
            
            return ResponseEntity.ok("Выданы все права для работы с колонками. Текущие права: " + rights);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }
} 