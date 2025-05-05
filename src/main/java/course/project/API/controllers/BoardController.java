package course.project.API.controllers;

import course.project.API.dto.board.BoardDTO;
import course.project.API.dto.board.BoardWithColumnsDTO;
import course.project.API.services.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    @Autowired
    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public ResponseEntity<List<BoardDTO>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<BoardDTO>> getBoardsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(boardService.getBoardsByProjectId(projectId));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardWithColumnsDTO> getBoardById(@PathVariable Long boardId) {
        return boardService.getBoardWithDetails(boardId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BoardDTO> createBoard(@RequestBody BoardDTO boardDTO) {
        return boardService.createBoard(boardDTO)
                .map(board -> ResponseEntity.status(HttpStatus.CREATED).body(board))
                .orElse(ResponseEntity.badRequest().build());
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<Void> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardDTO boardDTO) {
        
        boolean updated = boardService.updateBoard(boardId, boardDTO);
        if (updated) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{boardId}/participants/{userId}")
    public ResponseEntity<Void> addParticipant(
            @PathVariable Long boardId,
            @PathVariable Long userId) {
        
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
            @PathVariable Long userId) {
        
        boolean removed = boardService.removeParticipant(boardId, userId);
        if (removed) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 