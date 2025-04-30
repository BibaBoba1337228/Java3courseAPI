package course.project.API.services;

import course.project.API.dto.board.BoardDTO;
import course.project.API.dto.board.TagDTO;
import course.project.API.models.Board;
import course.project.API.models.Project;
import course.project.API.models.Tag;
import course.project.API.models.User;
import course.project.API.repositories.BoardRepository;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.TagRepository;
import course.project.API.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final TagRepository tagRepository;
    private final PermissionManagementService permissionManagementService;

    @Autowired
    public BoardService(BoardRepository boardRepository, ProjectRepository projectRepository, 
                        UserRepository userRepository, TagRepository tagRepository,
                        PermissionManagementService permissionManagementService) {
        this.boardRepository = boardRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.permissionManagementService = permissionManagementService;
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
                    
                    // Add tags if provided
                    if (boardDTO.getTags() != null) {
                        for (TagDTO tagDTO : boardDTO.getTags()) {
                            Tag tag = new Tag(tagDTO.getName(), tagDTO.getColor(), board);
                            board.addTag(tag);
                        }
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
                    
                    // Update tags if provided
                    if (boardDTO.getTags() != null) {
                        // We don't remove existing tags to avoid breaking task references
                        for (TagDTO tagDTO : boardDTO.getTags()) {
                            if (tagDTO.getId() != null) {
                                // Update existing tag
                                tagRepository.findById(tagDTO.getId()).ifPresent(tag -> {
                                    tag.setName(tagDTO.getName());
                                    tag.setColor(tagDTO.getColor());
                                    tagRepository.save(tag);
                                });
                            } else {
                                // Create new tag
                                Tag tag = new Tag(tagDTO.getName(), tagDTO.getColor(), board);
                                board.addTag(tag);
                            }
                        }
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

    @Transactional(readOnly = true)
    public List<User> getBoardParticipants(Long boardId) {
        return boardRepository.findById(boardId)
                .map(board -> new ArrayList<>(board.getParticipants()))
                .orElse(new ArrayList<>());
    }

    @Transactional(readOnly = true)
    public List<User> getAvailableUsersForBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        // Получаем всех участников проекта
        Set<User> projectParticipants = board.getProject().getParticipants();
        
        // Получаем текущих участников доски
        Set<User> boardParticipants = board.getParticipants();
        
        // Возвращаем участников проекта, которые еще не добавлены на доску
        return projectParticipants.stream()
                .filter(user -> !boardParticipants.contains(user))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TagDTO> getBoardTags(Long boardId) {
        return tagRepository.findByBoardId(boardId).stream()
                .map(this::convertTagToDTO)
                .collect(Collectors.toList());
    }
    
    private TagDTO convertTagToDTO(Tag tag) {
        return new TagDTO(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tag.getBoard().getId()
        );
    }

    private BoardDTO convertToDTO(Board board) {
        Set<Long> participantIds = board.getParticipants().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
                
        List<TagDTO> tagDTOs = board.getTags().stream()
                .map(this::convertTagToDTO)
                .collect(Collectors.toList());
                
        return new BoardDTO(
                board.getId(),
                board.getTitle(),
                board.getDescription(),
                board.getProject().getId(),
                participantIds,
                tagDTOs
        );
    }

    public Board createBoard(Board board, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        board.setProject(project);
        board = boardRepository.save(board);
        
        // Assign default permissions
        permissionManagementService.assignDefaultBoardPermissions(board);
        
        return board;
    }
} 