package course.project.API.services;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.board.*;
import course.project.API.dto.board.BoardWithColumnsDTO;
import course.project.API.models.Board;
import course.project.API.models.BoardRight;
import course.project.API.models.DashBoardColumn;
import course.project.API.models.Project;
import course.project.API.models.ProjectRight;
import course.project.API.models.ProjectUserRight;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import course.project.API.dto.user.UserResponse;
import course.project.API.dto.board.ColumnWithTasksDTO;
import course.project.API.dto.board.TagDTO;
import course.project.API.dto.board.TaskDTO;
import course.project.API.dto.board.ChecklistItemDTO;
import course.project.API.dto.board.AttachmentDTO;

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

    @Transactional(readOnly = true)
    public Optional<BoardDTO> getBoardById(Long id) {
        return boardRepository.findById(id)
                .map(board -> {
                    BoardDTO boardDTO = modelMapper.map(board, BoardDTO.class);
                    boardDTO.setProjectId(board.getProject().getId());
                    return boardDTO;
                });
    }

    @Transactional
    public Optional<BoardDTO> createBoard(BoardDTO boardDTO) {
        return projectRepository.findById(boardDTO.getProjectId())
                .flatMap(project -> {
                    // Создаем и настраиваем Board
                    Board board = new Board(
                        boardDTO.getTitle(), 
                        boardDTO.getDescription(), 
                        boardDTO.getEmoji(),
                        project
                    );
                    
                    // Always add project owner to board with all rights
                    User projectOwner = project.getOwner();
                    if (projectOwner != null) {
                        board.addParticipant(projectOwner);
                        // Grant all rights to the project owner
                        for (BoardRight right : BoardRight.values()) {
                            board.addUserRight(projectOwner, right);
                        }
                    }
                    
                    // Only add specified participants, not all project participants
                    if (boardDTO.getParticipantIds() != null) {
                        boardDTO.getParticipantIds().forEach(userId -> {
                            userRepository.findById(userId).ifPresent(user -> {
                                board.addParticipant(user);
                                // Give the user basic rights on this board
                                board.addUserRight(user, BoardRight.VIEW_BOARD);
                            });
                        });
                    }
                    
                    // Add users with ACCESS_ALL_BOARDS marker to this board
                    addUsersWithAllBoardsAccessToBoard(project, board);
                    
                    Board savedBoard = boardRepository.save(board);
                    
                    // Create default columns for the board
                    createDefaultColumns(savedBoard);
                    
                    // Create board DTO for response
                    BoardDTO createdBoardDTO = modelMapper.map(savedBoard, BoardDTO.class);
                    createdBoardDTO.setProjectId(project.getId());
                    
                    return Optional.of(createdBoardDTO);
                });
    }

    /**
     * Creates default columns for a newly created board
     * @param board The board to create columns for
     */
    private void createDefaultColumns(Board board) {
        // Create default columns: "To Do", "In Progress", "Done"
        DashBoardColumn todoColumn = new DashBoardColumn("To Do", board, 0);
        DashBoardColumn inProgressColumn = new DashBoardColumn("In Progress", board, 1);
        DashBoardColumn doneColumn = new DashBoardColumn("Done", board, 2);
        
        dashboardColumnRepository.save(todoColumn);
        dashboardColumnRepository.save(inProgressColumn);
        dashboardColumnRepository.save(doneColumn);
        
        // Add columns to the board
        board.addColumn(todoColumn);
        board.addColumn(inProgressColumn);
        board.addColumn(doneColumn);
    }

    /**
     * Вспомогательный метод для добавления пользователей с маркером ACCESS_ALL_BOARDS на доску
     */
    private void addUsersWithAllBoardsAccessToBoard(Project project, Board board) {
        try {
            // Получаем список пользователей проекта с маркером ACCESS_ALL_BOARDS
            List<ProjectUserRight> allBoardsAccessRights = project.getUserRights()
                .stream()
                .filter(right -> right.getRight() == ProjectRight.ACCESS_ALL_BOARDS)
                .collect(Collectors.toList());
            
            // Добавляем каждого пользователя с маркером на доску
            for (ProjectUserRight right : allBoardsAccessRights) {
                User user = right.getUser();
                
                // Пропускаем, если пользователь уже участник доски
                if (board.getParticipants().contains(user)) {
                    continue;
                }
                
                // Добавляем пользователя как участника доски
                board.addParticipant(user);
                
                // Выдаем базовое право просмотра доски
                board.addUserRight(user, BoardRight.VIEW_BOARD);
                
                logger.debug("Автоматически добавлен пользователь {} на доску {} из-за маркера ACCESS_ALL_BOARDS", 
                    user.getUsername(), board.getTitle());
            }
        } catch (Exception e) {
            logger.error("Ошибка при добавлении пользователей с маркером ACCESS_ALL_BOARDS: {}", e.getMessage(), e);
            // Не выбрасываем исключение, чтобы не прервать основной процесс создания доски
        }
    }

    @Transactional
    public Optional<BoardDTO> updateBoard(Long id, BoardDTO boardDTO) {
        return boardRepository.findById(id)
                .flatMap(board -> {
                    board.setTitle(boardDTO.getTitle());
                    board.setDescription(boardDTO.getDescription());
                    board.setEmoji(boardDTO.getEmoji());
                    
                    Board updatedBoard = boardRepository.save(board);
                    
                    // Create board DTO for response
                    BoardDTO updatedBoardDTO = modelMapper.map(updatedBoard, BoardDTO.class);
                    updatedBoardDTO.setProjectId(updatedBoard.getProject().getId());
                    
                    return Optional.of(updatedBoardDTO);
                });
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
                .map(board -> {
                    BoardWithColumnsDTO dto = new BoardWithColumnsDTO();
                    dto.setId(board.getId());
                    dto.setTitle(board.getTitle());
                    dto.setDescription(board.getDescription());
                    dto.setEmoji(board.getEmoji());
                    dto.setProjectId(board.getProject() != null ? board.getProject().getId() : null);

                    // Преобразуем участников
                    Set<UserResponse> participants = board.getParticipants().stream()
                        .map(user -> new UserResponse(
                                user.getId(),
                                user.getName(),
                                user.getAvatarURL()
                        ))
                        .collect(Collectors.toSet());
                    dto.setParticipants(participants);

                    // Преобразуем колонки и сортируем их по position
                    List<ColumnWithTasksDTO> columns = board.getColumns().stream()
                        .sorted(Comparator.comparing(DashBoardColumn::getPosition, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(column -> {
                            ColumnWithTasksDTO colDto = new ColumnWithTasksDTO();
                            colDto.setId(column.getId());
                            colDto.setName(column.getName());
                            colDto.setBoardId(board.getId());
                            colDto.setPosition(column.getPosition());
                            
                            // Загружаем задачи для колонки и сортируем их по позиции
                            Set<TaskDTO> tasks = column.getTasks().stream()
                                .sorted(Comparator.comparing(Task::getPosition, Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(task -> {
                                    TaskDTO taskDto = new TaskDTO();
                                    taskDto.setId(task.getId());
                                    taskDto.setTitle(task.getTitle());
                                    taskDto.setDescription(task.getDescription());
                                    taskDto.setColumnId(column.getId());
                                    taskDto.setPosition(task.getPosition());
                                    
                                    // Добавляем даты, если они есть
                                    if (task.getStartDate() != null) {
                                        taskDto.setStartDate(task.getStartDate());
                                    }
                                    if (task.getEndDate() != null) {
                                        taskDto.setEndDate(task.getEndDate());
                                    }
                                    
                                    // Добавляем тег, если он есть
                                    if (task.getTag() != null) {
                                        TagDTO tagDto = new TagDTO(
                                            task.getTag().getId(),
                                            task.getTag().getName(),
                                            task.getTag().getColor(),
                                            board.getId()
                                        );
                                        taskDto.setTag(tagDto);
                                    }
                                    
                                    // Добавляем участников задачи
                                    Set<UserResponse> taskParticipants = task.getParticipants().stream()
                                        .map(participant -> new UserResponse(
                                            participant.getId(),
                                            participant.getName(),
                                            participant.getAvatarURL()
                                        ))
                                        .collect(Collectors.toSet());
                                    taskDto.setParticipants(taskParticipants);

                                    // Convert checklist items to ChecklistItemDTO
                                    List<ChecklistItemDTO> checklistItems = task.getChecklist().stream()
                                        .map(item -> new ChecklistItemDTO(
                                            item.getId(),
                                            item.getText(),
                                            item.isCompleted(),
                                            item.getPosition()
                                        ))
                                        .collect(Collectors.toList());
                                    taskDto.setChecklist(checklistItems);
                                    
                                    // Convert attachments to AttachmentDTO
                                    List<AttachmentDTO> attachmentDTOs = task.getAttachments().stream()
                                        .map(attachment -> {
                                            AttachmentDTO attachmentDto = new AttachmentDTO(
                                                attachment.getId(),
                                                attachment.getFileName(),
                                                null, // Don't expose file path to client
                                                attachment.getFileType(),
                                                attachment.getFileSize(),
                                                attachment.getUploadedBy(),
                                                attachment.getUploadedAt()
                                            );
                                            // Set download URL
                                            attachmentDto.setDownloadUrl("http://localhost:8080/api/attachments/" + attachment.getId() + "/download");
                                            return attachmentDto;
                                        })
                                        .collect(Collectors.toList());
                                    taskDto.setAttachments(attachmentDTOs);
                                    
                                    return taskDto;
                                })
                                .collect(Collectors.toSet());
                            
                            colDto.setTasks(tasks);
                            return colDto;
                        })
                        .collect(Collectors.toList());
                    dto.setColumns(columns);

                    // Преобразуем теги
                    Set<TagDTO> tags = board.getTags().stream()
                        .map(tag -> {
                            TagDTO tagDto = new TagDTO(tag.getId(), tag.getName(), tag.getColor(), board.getId());
                            return tagDto;
                        })
                        .collect(Collectors.toSet());
                    dto.setTags(tags);

                    return dto;
                });
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
            
            // Проверяем, является ли пользователь участником доски или владельцем проекта
            // Участник проекта НЕ должен автоматически иметь доступ к доске
            return board.getParticipants().contains(user) || 
                   (board.getProject() != null && board.getProject().getOwner() != null && 
                    board.getProject().getOwner().getId().equals(user.getId()));
        } catch (Exception e) {
            logger.error("Ошибка при проверке доступа пользователя к доске: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Добавляет пользователя на все доски проекта и выдает базовые права
     * 
     * @param projectId ID проекта
     * @param userId ID пользователя
     * @return количество досок, на которые добавлен пользователь
     */
    @Transactional
    public int addUserToAllProjectBoards(Long projectId, Long userId) {
        try {
            // Проверяем существование пользователя и проекта
            var user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден с ID: " + userId));
                
            var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Проект не найден с ID: " + projectId));
            
            // Проверяем, что пользователь является участником проекта
            if (!project.getParticipants().contains(user) && !project.getOwner().equals(user)) {
                throw new IllegalArgumentException("Пользователь не является участником проекта");
            }
            
            // Получаем все доски проекта
            List<Board> boards = boardRepository.findByProjectId(projectId);
            
            // Счетчик добавленных досок
            int addedCount = 0;
            
            // Добавляем пользователя на каждую доску
            for (Board board : boards) {
                // Пропускаем, если пользователь уже участник
                if (board.getParticipants().contains(user)) {
                    continue;
                }
                
                // Добавляем пользователя как участника доски
                board.addParticipant(user);
                
                // Выдаем базовое право просмотра доски
                board.addUserRight(user, BoardRight.VIEW_BOARD);
                
                // Сохраняем доску
                boardRepository.save(board);
                addedCount++;
            }
            
            logger.info("Пользователь {} добавлен на {} досок проекта {}", userId, addedCount, projectId);
            return addedCount;
        } catch (Exception e) {
            logger.error("Ошибка при добавлении пользователя на все доски проекта: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Добавляет пользователя на все доски проекта по имени пользователя
     * и устанавливает маркер ACCESS_ALL_BOARDS
     * 
     * @param projectId ID проекта
     * @param username имя пользователя
     * @return количество досок, на которые добавлен пользователь
     */
    @Transactional
    public int addUserToAllProjectBoardsByUsername(Long projectId, String username) {
        try {
            // Проверяем существование пользователя и проекта
            var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден с именем: " + username));
                
            var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Проект не найден с ID: " + projectId));
            
            // Проверяем, что пользователь является участником проекта
            if (!project.getParticipants().contains(user) && !project.getOwner().equals(user)) {
                throw new IllegalArgumentException("Пользователь не является участником проекта");
            }
            
            // Устанавливаем маркер ACCESS_ALL_BOARDS
            project.addUserRight(user, ProjectRight.ACCESS_ALL_BOARDS);
            projectRepository.save(project);
            
            // Получаем все доски проекта
            List<Board> boards = boardRepository.findByProjectId(projectId);
            
            // Счетчик добавленных досок
            int addedCount = 0;
            
            // Добавляем пользователя на каждую доску
            for (Board board : boards) {
                // Пропускаем, если пользователь уже участник
                if (board.getParticipants().contains(user)) {
                    continue;
                }
                
                // Добавляем пользователя как участника доски
                board.addParticipant(user);
                
                // Выдаем базовое право просмотра доски
                board.addUserRight(user, BoardRight.VIEW_BOARD);
                
                // Сохраняем доску
                boardRepository.save(board);
                addedCount++;
            }
            
            logger.info("Пользователь {} добавлен на {} досок проекта {} и отмечен маркером ACCESS_ALL_BOARDS", 
                username, addedCount, projectId);
            return addedCount;
        } catch (Exception e) {
            logger.error("Ошибка при добавлении пользователя на все доски проекта: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Удаляет пользователя со всех досок проекта по имени пользователя
     * и снимает маркер ACCESS_ALL_BOARDS
     * 
     * @param projectId ID проекта
     * @param username имя пользователя
     * @return количество досок, с которых удален пользователь
     */
    @Transactional
    public int removeUserFromAllProjectBoardsByUsername(Long projectId, String username) {
        try {
            // Проверяем существование пользователя и проекта
            var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден с именем: " + username));
                
            var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Проект не найден с ID: " + projectId));
            
            // Не позволяем удалить владельца проекта с досок
            if (project.getOwner().equals(user)) {
                throw new IllegalArgumentException("Нельзя удалить владельца проекта с досок");
            }
            
            // Снимаем маркер ACCESS_ALL_BOARDS
            project.removeUserRight(user, ProjectRight.ACCESS_ALL_BOARDS);
            projectRepository.save(project);
            
            // Получаем все доски проекта
            List<Board> boards = boardRepository.findByProjectId(projectId);
            
            // Счетчик досок, с которых удален пользователь
            int removedCount = 0;
            
            // Удаляем пользователя с каждой доски
            for (Board board : boards) {
                // Пропускаем, если пользователь не участник
                if (!board.getParticipants().contains(user)) {
                    continue;
                }
                
                // Удаляем пользователя с доски
                board.removeParticipant(user);
                
                // Сохраняем доску
                boardRepository.save(board);
                removedCount++;
            }
            
            logger.info("Пользователь {} удален с {} досок проекта {} и с него снят маркер ACCESS_ALL_BOARDS", 
                username, removedCount, projectId);
            return removedCount;
        } catch (Exception e) {
            logger.error("Ошибка при удалении пользователя со всех досок проекта: {}", e.getMessage(), e);
            throw e;
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
                // Если position не задан, устанавливаем его в конец списка
                Integer finalPosition = position;
                if (finalPosition == null) {
                    int maxPosition = board.getColumns().stream()
                        .filter(col -> col.getPosition() != null)
                        .map(DashBoardColumn::getPosition)
                        .max(Integer::compare)
                        .orElse(-1);
                    finalPosition = maxPosition + 1;
                }
                
                DashBoardColumn column = new DashBoardColumn(title, board, finalPosition);
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
                    try {
                        for (Map<String, Object> columnData : columns) {
                            Long columnId = ((Number) columnData.get("id")).longValue();
                            Integer position = ((Number) columnData.get("position")).intValue();
                            
                            dashboardColumnRepository.findById(columnId)
                                    .ifPresent(column -> {
                                        if (column.getBoard().getId().equals(boardId)) {
                                            column.setPosition(position);
                                        }
                                    });
                        }
                        return true;
                    } catch (Exception e) {
                        logger.error("Error reordering columns: {}", e.getMessage(), e);
                        return false;
                    }
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

    /**
     * Получает все задачи пользователя с досок, на которых он состоит, по ID проекта
     *
     * @param projectId ID проекта
     * @param userId ID пользователя
     * @return список задач пользователя в указанном проекте
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getUserTasksByProjectId(Long projectId, Long userId) {
        try {
            // Получаем пользователя и проект
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден с ID: " + userId));
            
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Проект не найден с ID: " + projectId));
            
            // Проверяем, что пользователь имеет доступ к проекту
            boolean isProjectMember = project.getParticipants().contains(user) || 
                                    project.getOwner().equals(user);
            if (!isProjectMember) {
                throw new IllegalArgumentException("Пользователь не является участником проекта");
            }
            
            // Получаем все доски проекта
            List<Board> allBoards = boardRepository.findByProjectId(projectId);
            
            // Фильтруем доски, где пользователь является участником
            List<Board> userBoards = allBoards.stream()
                .filter(board -> board.getParticipants().contains(user) || 
                               project.getOwner().equals(user))
                .collect(Collectors.toList());
            
            // Собираем все задачи с этих досок
            List<TaskDTO> tasks = new ArrayList<>();
            
            for (Board board : userBoards) {
                // Получаем все колонки доски
                for (DashBoardColumn column : board.getColumns()) {
                    // Преобразуем задачи в DTO и добавляем к результату
                    column.getTasks().forEach(task -> {
                        TaskDTO taskDto = new TaskDTO();
                        taskDto.setId(task.getId());
                        taskDto.setTitle(task.getTitle());
                        taskDto.setDescription(task.getDescription());
                        taskDto.setColumnId(column.getId());
                        taskDto.setPosition(task.getPosition());
                        
                        // Добавляем информацию о доске
                        taskDto.setBoardId(board.getId());
                        taskDto.setBoardTitle(board.getTitle());
                        
                        // Добавляем колонку
                        taskDto.setColumnName(column.getName());
                        
                        // Добавляем даты, если они есть
                        if (task.getStartDate() != null) {
                            taskDto.setStartDate(task.getStartDate());
                        }
                        if (task.getEndDate() != null) {
                            taskDto.setEndDate(task.getEndDate());
                        }
                        
                        // Добавляем тег, если он есть
                        if (task.getTag() != null) {
                            TagDTO tagDto = new TagDTO(
                                task.getTag().getId(),
                                task.getTag().getName(),
                                task.getTag().getColor(),
                                board.getId()
                            );
                            taskDto.setTag(tagDto);
                        }
                        
                        // Добавляем участников задачи
                        Set<UserResponse> taskParticipants = task.getParticipants().stream()
                            .map(participant -> new UserResponse(
                                participant.getId(),
                                participant.getName(),
                                participant.getAvatarURL()
                            ))
                            .collect(Collectors.toSet());
                        taskDto.setParticipants(taskParticipants);

                        // Convert checklist items to ChecklistItemDTO
                        List<ChecklistItemDTO> checklistItems = task.getChecklist().stream()
                            .map(item -> new ChecklistItemDTO(
                                item.getId(),
                                item.getText(),
                                item.isCompleted(),
                                item.getPosition()
                            ))
                            .collect(Collectors.toList());
                        taskDto.setChecklist(checklistItems);
                        
                        tasks.add(taskDto);
                    });
                }
            }
            
            return tasks;
        } catch (Exception e) {
            logger.error("Ошибка при получении задач пользователя по проекту: {}", e.getMessage(), e);
            throw e;
        }
    }
} 