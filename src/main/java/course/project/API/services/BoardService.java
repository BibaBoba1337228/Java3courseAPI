package course.project.API.services;

import course.project.API.dto.board.BoardDTO;
import course.project.API.models.Board;
import course.project.API.models.Project;
import course.project.API.models.User;
import course.project.API.repositories.BoardRepository;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BoardService {
    private final BoardRepository boardRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Autowired
    public BoardService(BoardRepository boardRepository, ProjectRepository projectRepository, UserRepository userRepository) {
        this.boardRepository = boardRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public List<BoardDTO> getAllBoards() {
        return boardRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BoardDTO> getBoardsByProjectId(Long projectId) {
        return boardRepository.findByProjectId(projectId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<BoardDTO> getBoardById(Long id) {
        return boardRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Transactional
    public Optional<BoardDTO> createBoard(BoardDTO boardDTO) {
        return projectRepository.findById(boardDTO.getProjectId())
                .map(project -> {
                    Board board = new Board(boardDTO.getTitle(), boardDTO.getDescription(), project);
                    if (boardDTO.getParticipantIds() != null) {
                        Set<User> participants = new HashSet<>();
                        for (Long userId : boardDTO.getParticipantIds()) {
                            userRepository.findById(userId).ifPresent(participants::add);
                        }
                        board.setParticipants(participants);
                    }
                    return convertToDTO(boardRepository.save(board));
                });
    }

    @Transactional
    public Optional<BoardDTO> updateBoard(Long id, BoardDTO boardDTO) {
        return boardRepository.findById(id)
                .map(board -> {
                    board.setTitle(boardDTO.getTitle());
                    board.setDescription(boardDTO.getDescription());
                    if (boardDTO.getParticipantIds() != null) {
                        Set<User> participants = new HashSet<>();
                        for (Long userId : boardDTO.getParticipantIds()) {
                            userRepository.findById(userId).ifPresent(participants::add);
                        }
                        board.setParticipants(participants);
                    }
                    return convertToDTO(boardRepository.save(board));
                });
    }

    @Transactional
    public void deleteBoard(Long id) {
        boardRepository.deleteById(id);
    }

    @Transactional
    public Optional<BoardDTO> addParticipant(Long boardId, Long userId) {
        return boardRepository.findById(boardId)
                .flatMap(board -> userRepository.findById(userId)
                        .map(user -> {
                            board.addParticipant(user);
                            return convertToDTO(boardRepository.save(board));
                        }));
    }

    @Transactional
    public Optional<BoardDTO> removeParticipant(Long boardId, Long userId) {
        return boardRepository.findById(boardId)
                .flatMap(board -> userRepository.findById(userId)
                        .map(user -> {
                            board.removeParticipant(user);
                            return convertToDTO(boardRepository.save(board));
                        }));
    }

    private BoardDTO convertToDTO(Board board) {
        Set<Long> participantIds = board.getParticipants().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        return new BoardDTO(
                board.getId(),
                board.getTitle(),
                board.getDescription(),
                board.getProject().getId(),
                participantIds
        );
    }
} 