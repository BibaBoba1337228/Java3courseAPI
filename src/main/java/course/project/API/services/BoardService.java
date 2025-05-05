package course.project.API.services;

import course.project.API.dto.board.*;
import course.project.API.dto.board.BoardWithColumnsDTO;
import course.project.API.models.Board;
import course.project.API.models.DashBoardColumn;
import course.project.API.models.Tag;
import course.project.API.models.Task;
import course.project.API.models.User;
import course.project.API.repositories.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BoardService {
    private final BoardRepository boardRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final DashBoardColumnRepository dashboardColumnRepository;
    private final ModelMapper modelMapper;
    private static final Logger logger = LoggerFactory.getLogger(BoardService.class);

    @Autowired
    public BoardService(BoardRepository boardRepository, ProjectRepository projectRepository, 
                        UserRepository userRepository, TagRepository tagRepository,
                        DashBoardColumnRepository dashboardColumnRepository,
                        ModelMapper modelMapper) {
        this.boardRepository = boardRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.dashboardColumnRepository = dashboardColumnRepository;
        this.modelMapper = modelMapper;
    }

    public List<BoardDTO> getAllBoards() {
        return boardRepository.findAll().stream()
                .map(board -> modelMapper.map(board, BoardDTO.class))
                .collect(Collectors.toList());
    }

    public List<BoardDTO> getBoardsByProjectId(Long projectId) {
        return boardRepository.findByProjectId(projectId).stream()
                .map(board -> modelMapper.map(board, BoardDTO.class))
                .collect(Collectors.toList());
    }

    public Optional<BoardDTO> getBoardById(Long id) {
        return boardRepository.findById(id)
                .map(board -> modelMapper.map(board, BoardDTO.class));
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

                    if (boardDTO.getTags() != null) {
                        for (TagDTO tagDTO : boardDTO.getTags()) {
                            Tag tag = new Tag(tagDTO.getName(), tagDTO.getColor(), board);
                            board.addTag(tag);
                        }
                    }
                    
                    Board savedBoard = boardRepository.save(board);
                    return modelMapper.map(savedBoard, BoardDTO.class);
                });
    }

    @Transactional
    public boolean updateBoard(Long id, BoardDTO boardDTO) {
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
                    
                    boardRepository.save(board);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public void deleteBoard(Long id) {
        boardRepository.deleteById(id);
    }

    @Transactional
    public boolean addParticipant(Long boardId, Long userId) {
        return boardRepository.findById(boardId)
                .flatMap(board -> userRepository.findById(userId)
                        .map(user -> {
                            board.addParticipant(user);
                            boardRepository.save(board);
                            return true;
                        }))
                .orElse(false);
    }

    @Transactional
    public boolean removeParticipant(Long boardId, Long userId) {
        return boardRepository.findById(boardId)
                .flatMap(board -> userRepository.findById(userId)
                        .map(user -> {
                            board.removeParticipant(user);
                            boardRepository.save(board);
                            return true;
                        }))
                .orElse(false);
    }
    
    @Transactional(readOnly = true)
    public List<TagDTO> getBoardTags(Long boardId) {
        return tagRepository.findByBoardId(boardId).stream()
                .map(tag -> modelMapper.map(tag, TagDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<BoardWithColumnsDTO> getBoardWithDetails(Long id) {
        return boardRepository.findById(id)
                .map(board -> modelMapper.map(board, BoardWithColumnsDTO.class));
    }

    /**
     * Проверяет, имеет ли пользователь доступ к доске
     * 
     * @param username имя пользователя
     * @param boardId идентификатор доски
     * @return true, если пользователь имеет доступ к доске
     */
    @Transactional(readOnly = true)
    public boolean hasUserAccessToBoard(String username, Long boardId) {
        try {
            // Получаем пользователя и доску
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + username));
            
            Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new NoSuchElementException("Доска не найдена: " + boardId));
            
            // Проверяем, является ли пользователь участником доски
            return board.getParticipants().contains(user) || 
                   (board.getProject() != null && board.getProject().getParticipants().contains(user)) ||
                   (board.getProject() != null && board.getProject().getOwner() != null && 
                    board.getProject().getOwner().getId().equals(user.getId()));
        } catch (Exception e) {
            logger.error("Ошибка при проверке доступа пользователя к доске: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Создает новую колонку на доске
     * 
     * @param boardId ID доски
     * @param title название колонки
     * @param position позиция колонки
     * @return созданная колонка или null в случае ошибки
     */
    @Transactional
    public DashBoardColumn createColumn(Long boardId, String title, Integer position) {
        try {
            return boardRepository.findById(boardId).map(board -> {
                DashBoardColumn column = new DashBoardColumn(title, board, position);
                return dashboardColumnRepository.save(column);
            }).orElse(null);
        } catch (Exception e) {
            logger.error("Ошибка при создании колонки: {}", e.getMessage(), e);
            return null;
        }
    }

    @Transactional
    public DashBoardColumn updateColumn(Long boardId, Long columnId, String title, Integer position) {
        return boardRepository.findById(boardId)
                .flatMap(board -> dashboardColumnRepository.findById(columnId)
                        .map(column -> {
                            if (title != null) {
                                column.setName(title);
                            }
                            if (position != null) {
                                column.setPosition(position);
                            }
                            return dashboardColumnRepository.save(column);
                        }))
                .orElse(null);
    }

    @Transactional
    public boolean deleteColumn(Long boardId, Long columnId) {
        return boardRepository.findById(boardId)
                .flatMap(board -> dashboardColumnRepository.findById(columnId)
                        .map(column -> {
                            board.removeColumn(column);
                            boardRepository.save(board);
                            return true;
                        }))
                .orElse(false);
    }

    @Transactional
    public boolean reorderColumns(Long boardId, List<Map<String, Object>> columns) {
        return boardRepository.findById(boardId)
                .map(board -> {
                    for (Map<String, Object> columnData : columns) {
                        Long columnId = Long.valueOf(columnData.get("id").toString());
                        Integer position = (Integer) columnData.get("position");
                        
                        dashboardColumnRepository.findById(columnId)
                                .ifPresent(column -> {
                                    column.setPosition(position);
                                    dashboardColumnRepository.save(column);
                                });
                    }
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public TaskDTO createTask(Long boardId, Long columnId, String title, String description, List<Long> tagIds) {
        return boardRepository.findById(boardId)
                .flatMap(board -> dashboardColumnRepository.findById(columnId)
                        .map(column -> {
                            Task task = new Task(title, description, column);
                            
                            if (tagIds != null) {
                                for (Long tagId : tagIds) {
                                    tagRepository.findById(tagId)
                                            .ifPresent(task::setTag);
                                }
                            }
                            
                            column.addTask(task);
                            dashboardColumnRepository.save(column);
                            return modelMapper.map(task, TaskDTO.class);
                        }))
                .orElse(null);
    }

    @Transactional
    public TaskDTO updateTask(Long boardId, Long taskId, String title, String description, List<Long> tagIds) {
        return boardRepository.findById(boardId)
                .flatMap(board -> dashboardColumnRepository.findByBoard_Id(boardId).stream()
                        .flatMap(column -> column.getTasks().stream())
                        .filter(task -> task.getId().equals(taskId))
                        .findFirst()
                        .map(task -> {
                            if (title != null) {
                                task.setTitle(title);
                            }
                            if (description != null) {
                                task.setDescription(description);
                            }
                            if (tagIds != null) {
                                task.setTag(null); // Clear existing tag
                                if (!tagIds.isEmpty()) {
                                    tagRepository.findById(tagIds.get(0))
                                            .ifPresent(task::setTag);
                                }
                            }
                            return modelMapper.map(task, TaskDTO.class);
                        }))
                .orElse(null);
    }

    @Transactional
    public boolean deleteTask(Long boardId, Long taskId) {
        return boardRepository.findById(boardId)
                .flatMap(board -> dashboardColumnRepository.findByBoard_Id(boardId).stream()
                        .filter(column -> column.getTasks().stream()
                                .anyMatch(task -> task.getId().equals(taskId)))
                        .findFirst()
                        .map(column -> {
                            column.getTasks().removeIf(task -> task.getId().equals(taskId));
                            dashboardColumnRepository.save(column);
                            return true;
                        }))
                .orElse(false);
    }

    @Transactional
    public boolean moveTask(Long boardId, Long taskId, Long sourceColumnId, Long targetColumnId, Integer newPosition) {
        return boardRepository.findById(boardId)
                .flatMap(board -> {
                    DashBoardColumn sourceColumn = dashboardColumnRepository.findById(sourceColumnId).orElse(null);
                    DashBoardColumn targetColumn = dashboardColumnRepository.findById(targetColumnId).orElse(null);
                    
                    if (sourceColumn == null || targetColumn == null) {
                        return Optional.empty();
                    }
                    
                    return sourceColumn.getTasks().stream()
                            .filter(task -> task.getId().equals(taskId))
                            .findFirst()
                            .map(task -> {
                                sourceColumn.getTasks().remove(task);
                                task.setColumn(targetColumn);
                                if (newPosition != null) {
                                    task.setPosition(newPosition);
                                }
                                targetColumn.addTask(task);
                                dashboardColumnRepository.save(sourceColumn);
                                dashboardColumnRepository.save(targetColumn);
                                return true;
                            });
                })
                .orElse(false);
    }
} 