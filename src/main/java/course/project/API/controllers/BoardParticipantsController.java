package course.project.API.controllers;

import course.project.API.models.User;
import course.project.API.repositories.UserRepository;
import course.project.API.services.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/boards/{boardId}/participants")
public class BoardParticipantsController {

    private final BoardService boardService;
    private final UserRepository userRepository;

    @Autowired
    public BoardParticipantsController(BoardService boardService, UserRepository userRepository) {
        this.boardService = boardService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Set<User>> getBoardParticipants(@PathVariable Long boardId) {
        return boardService.getBoardById(boardId)
                .map(boardDTO -> {
                    Set<User> participants = boardDTO.getParticipantIds().stream()
                            .map(userId -> userRepository.findById(userId).orElse(null))
                            .filter(user -> user != null)
                            .collect(Collectors.toSet());
                    return ResponseEntity.ok(participants);
                })
                .orElse(ResponseEntity.notFound().build());
    }

//    @PostMapping("/{userId}")
//    public ResponseEntity<?> addParticipantToBoard(
//            @PathVariable Long boardId,
//            @PathVariable Long userId) {
//
//        return boardService.addParticipant(boardId, userId)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @DeleteMapping("/{userId}")
//    public ResponseEntity<?> removeParticipantFromBoard(
//            @PathVariable Long boardId,
//            @PathVariable Long userId) {
//
//        return boardService.removeParticipant(boardId, userId)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
} 