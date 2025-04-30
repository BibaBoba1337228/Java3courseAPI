package course.project.API.controllers;

import course.project.API.dto.board.BoardDTO;
import course.project.API.dto.board.TagDTO;
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
    public ResponseEntity<BoardDTO> getBoardById(@PathVariable Long boardId) {
        return boardService.getBoardById(boardId)
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
    public ResponseEntity<BoardDTO> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardDTO boardDTO) {
        
        return boardService.updateBoard(boardId, boardDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }
} 