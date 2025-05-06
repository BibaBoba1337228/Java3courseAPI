package course.project.API.controllers;

import course.project.API.dto.RightDto;
import course.project.API.models.Board;
import course.project.API.models.BoardRight;
import course.project.API.models.ProjectRight;
import course.project.API.models.User;
import course.project.API.repositories.BoardRepository;
import course.project.API.services.BoardRightService;
import course.project.API.services.ProjectRightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/boards/{boardId}/rights")
public class BoardRightController {

    private final BoardRightService boardRightService;
    private final BoardRepository boardRepository;
    private final ProjectRightService projectRightService;

    @Autowired
    public BoardRightController(BoardRightService boardRightService, 
                              BoardRepository boardRepository,
                              ProjectRightService projectRightService) {
        this.boardRightService = boardRightService;
        this.boardRepository = boardRepository;
        this.projectRightService = projectRightService;
    }

    @PostMapping("/users")
    public ResponseEntity<?> addUserToBoard(
            @PathVariable Long boardId,
            @RequestBody Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_MEMBERS right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body("You don't have permission to add users to this board");
        }
        
        boardRightService.addUserToBoard(boardId, userId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/users/username")
    public ResponseEntity<?> addUserToBoardByUsername(
            @PathVariable Long boardId,
            @RequestBody String username,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_MEMBERS right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body("You don't have permission to add users to this board");
        }
        
        boardRightService.addUserToBoardByUsername(boardId, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Set<BoardRight>> getUserRights(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has VIEW_BOARD right or is requesting their own rights
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD) 
                && !currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body(null);
        }
        
        Set<BoardRight> rights = boardRightService.getUserBoardRights(boardId, userId);
        return ResponseEntity.ok(rights);
    }
    
    @GetMapping("/users/username/{username}")
    public ResponseEntity<Set<BoardRight>> getUserRightsByUsername(
            @PathVariable Long boardId,
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has VIEW_BOARD right or is requesting their own rights
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD) 
                && !currentUser.getUsername().equals(username)) {
            return ResponseEntity.status(403).body(null);
        }
        
        Set<BoardRight> rights = boardRightService.getUserBoardRightsByUsername(boardId, username);
        return ResponseEntity.ok(rights);
    }

    @PostMapping("/grant")
    public ResponseEntity<?> grantRight(
            @PathVariable Long boardId,
            @RequestBody RightDto rightDto,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_RIGHTS right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MANAGE_RIGHTS)) {
            return ResponseEntity.status(403).body("You don't have permission to manage rights on this board");
        }
        
        try {
            BoardRight right = BoardRight.valueOf(rightDto.getRightName());
            
            // If username is provided, use it instead of userId
            if (rightDto.getUsername() != null && !rightDto.getUsername().isEmpty()) {
                boardRightService.grantBoardRightByUsername(boardId, rightDto.getUsername(), right);
            } else if (rightDto.getUserId() != null) {
                boardRightService.grantBoardRight(boardId, rightDto.getUserId(), right);
            } else {
                return ResponseEntity.badRequest().body("Either userId or username must be provided");
            }
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid right name");
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revokeRight(
            @PathVariable Long boardId,
            @RequestBody RightDto rightDto,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_RIGHTS right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MANAGE_RIGHTS)) {
            return ResponseEntity.status(403).body("You don't have permission to manage rights on this board");
        }
        
        try {
            BoardRight right = BoardRight.valueOf(rightDto.getRightName());
            
            // If username is provided, use it instead of userId
            if (rightDto.getUsername() != null && !rightDto.getUsername().isEmpty()) {
                boardRightService.revokeBoardRightByUsername(boardId, rightDto.getUsername(), right);
            } else if (rightDto.getUserId() != null) {
                boardRightService.revokeBoardRight(boardId, rightDto.getUserId(), right);
            } else {
                return ResponseEntity.badRequest().body("Either userId or username must be provided");
            }
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid right name");
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> removeUserFromBoard(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_MEMBERS right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body("You don't have permission to remove users from this board");
        }
        
        boardRightService.removeUserFromBoard(boardId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/users/username/{username}")
    public ResponseEntity<?> removeUserFromBoardByUsername(
            @PathVariable Long boardId,
            @PathVariable String username,
            @AuthenticationPrincipal User currentUser) {
        
        // Check if current user has MANAGE_MEMBERS right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(403).body("You don't have permission to remove users from this board");
        }
        
        boardRightService.removeUserFromBoardByUsername(boardId, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/visible")
    public ResponseEntity<List<Board>> getVisibleBoards(
            @RequestParam Long projectId,
            @AuthenticationPrincipal User currentUser) {
        
        List<Board> boards = boardRightService.getVisibleBoardsForUser(projectId, currentUser.getId());
        return ResponseEntity.ok(boards);
    }
} 