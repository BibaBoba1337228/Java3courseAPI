package course.project.API.controllers;

import course.project.API.dto.board.BoardDTO;
import course.project.API.dto.board.TagDTO;
import course.project.API.models.User;
import course.project.API.services.BoardService;
import course.project.API.services.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.security.Principal;
import course.project.API.repositories.UserRepository;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @Autowired
    public BoardController(BoardService boardService, 
                         UserRepository userRepository,
                         PermissionService permissionService) {
        this.boardService = boardService;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<List<BoardDTO>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<BoardDTO>> getBoardsByProject(
            @PathVariable Long projectId,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasProjectAccess(user.getId(), projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(boardService.getBoardsByProjectId(projectId));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDTO> getBoardById(
            @PathVariable Long boardId,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasBoardAccess(user.getId(), boardId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return boardService.getBoardById(boardId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BoardDTO> createBoard(
            @RequestBody BoardDTO boardDTO,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasProjectAccess(user.getId(), boardDTO.getProjectId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return boardService.createBoard(boardDTO)
                .map(board -> ResponseEntity.status(HttpStatus.CREATED).body(board))
                .orElse(ResponseEntity.badRequest().build());
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<BoardDTO> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardDTO boardDTO,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageBoard(user.getId(), boardId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return boardService.updateBoard(boardId, boardDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long boardId,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageBoard(user.getId(), boardId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{boardId}/participants/{userId}")
    public ResponseEntity<Void> addParticipantToBoard(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            Principal principal) {
            
        var currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageBoardParticipants(currentUser.getId(), boardId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        boardService.addParticipant(boardId, userId)
            .orElseThrow(() -> new RuntimeException("Failed to add participant"));
            
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{boardId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipantFromBoard(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            Principal principal) {
            
        var currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageBoardParticipants(currentUser.getId(), boardId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        boardService.removeParticipant(boardId, userId)
            .orElseThrow(() -> new RuntimeException("Failed to remove participant"));
            
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{boardId}/participants")
    public ResponseEntity<List<User>> getBoardParticipants(
            @PathVariable Long boardId,
            Principal principal) {
            
        var currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canUpdateBoard(currentUser.getId(), boardId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<User> participants = boardService.getBoardParticipants(boardId);
        return ResponseEntity.ok(participants);
    }

    @GetMapping("/{boardId}/available-participants")
    public ResponseEntity<List<User>> getAvailableUsersForBoard(
            @PathVariable Long boardId,
            Principal principal) {
            
        var currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageBoardParticipants(currentUser.getId(), boardId, null)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<User> availableUsers = boardService.getAvailableUsersForBoard(boardId);
        return ResponseEntity.ok(availableUsers);
    }
} 