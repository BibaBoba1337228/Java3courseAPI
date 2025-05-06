package course.project.API.services;

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
                    Board board = new Board(boardDTO.getTitle(), boardDTO.getDescription(), project);
                    
                    // Only add specified participants, not all project participants
                    if (boardDTO.getParticipantIds() != null) {
                        Set<User> participants = new HashSet<>();
                        for (Long userId : boardDTO.getParticipantIds()) {
                            // Пропускаем null значения в списке participantIds
                            if (userId == null) {
                                continue;
                            }
                            
                            userRepository.findById(userId).ifPresent(user -> {
                                // Only add users that are already in the project
                                if (project.getParticipants().contains(user) || project.getOwner().equals(user)) {
                                    participants.add(user);
                                    // Grant VIEW_BOARD right to the participant
                                    board.addUserRight(user, BoardRight.VIEW_BOARD);
                                }
                            });
                        }
                        board.setParticipants(participants);
                    } else {
                        // If no participants specified, only add project owner
                        board.addParticipant(project.getOwner());
                        // Grant all rights to the project owner
                        for (BoardRight right : BoardRight.values()) {
                            board.addUserRight(project.getOwner(), right);
                        }
                    }

                    // Add users with ACCESS_ALL_BOARDS marker to this board
                    addUsersWithAllBoardsAccessToBoard(project, board);
                    
                    // Сохраняем доску перед созданием тегов
                    Board savedBoard = boardRepository.save(board);
                    
                    // Сохраняем теги в отдельном цикле для избежания проблем с final переменными
                    if (boardDTO.getTags() != null && !boardDTO.getTags().isEmpty()) {
                        for (TagDTO tagDTO : boardDTO.getTags()) {
                            // Создаем тег с сохраненной доской
                            Tag tag = new Tag(tagDTO.getName(), tagDTO.getColor(), savedBoard);
                            // Сохраняем тег
                            Tag savedTag = tagRepository.save(tag);
                            // Добавляем сохраненный тег к доске
                            savedBoard.getTags().add(savedTag);
                        }
                        // Обновляем доску с новыми тегами
                        savedBoard = boardRepository.save(savedBoard);
                    }
                    
                    // Формируем DTO для ответа
                    BoardDTO savedBoardDTO = modelMapper.map(savedBoard, BoardDTO.class);
                    savedBoardDTO.setProjectId(savedBoard.getProject().getId());
                    
                    // Устанавливаем boardId для каждого тега
                    if (savedBoardDTO.getTags() != null) {
                        final Long boardId = savedBoard.getId(); // используем final для использования в лямбде
                        savedBoardDTO.getTags().forEach(tag -> tag.setBoardId(boardId));
                    }
                    
                    return Optional.of(savedBoardDTO);
                });
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
        try {
            Board board = boardRepository.findById(id).orElse(null);
            if (board == null) {
                logger.error("Не удалось найти доску с id: {}", id);
                return Optional.empty();
            }
            
            // Обновляем основные поля
            board.setTitle(boardDTO.getTitle());
            board.setDescription(boardDTO.getDescription());
            
            // Обновляем участников, сохраняя существующие
            if (boardDTO.getParticipantIds() != null) {
                // Создаем новый список участников
                Set<User> updatedParticipants = new HashSet<>(board.getParticipants());
                
                // Добавляем новых участников из запроса
                for (Long userId : boardDTO.getParticipantIds()) {
                    if (userId != null) {
                        Optional<User> userOpt = userRepository.findById(userId);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            if (!updatedParticipants.contains(user)) {
                                updatedParticipants.add(user);
                                // Добавляем право просмотра новому участнику
                                board.addUserRight(user, BoardRight.VIEW_BOARD);
                            }
                        }
                    }
                }
                
                // Если включен владелец проекта, добавляем его в список
                if (board.getProject() != null && board.getProject().getOwner() != null) {
                    User projectOwner = board.getProject().getOwner();
                    updatedParticipants.add(projectOwner);
                }
                
                // Обновляем список участников
                board.setParticipants(updatedParticipants);
            }
            
            // Обновляем существующие теги или добавляем новые
            if (boardDTO.getTags() != null) {
                // Удаляем все существующие теги доски
                List<Tag> existingTags = new ArrayList<>(board.getTags());
                for (Tag tag : existingTags) {
                    board.removeTag(tag);
                    tagRepository.delete(tag);
                }
                
                // Добавляем новые теги из запроса
                for (TagDTO tagDTO : boardDTO.getTags()) {
                    Tag tag = new Tag(tagDTO.getName(), tagDTO.getColor(), board);
                    board.addTag(tag);
                    tagRepository.save(tag);
                }
            }
            
            // Сохраняем изменения доски
            Board savedBoard = boardRepository.save(board);
            
            // Преобразуем сохраненную доску в DTO
            BoardDTO savedBoardDTO = modelMapper.map(savedBoard, BoardDTO.class);
            savedBoardDTO.setProjectId(savedBoard.getProject().getId());
            
            // Устанавливаем boardId для каждого тега
            if (savedBoardDTO.getTags() != null) {
                final Long boardId = savedBoard.getId();
                savedBoardDTO.getTags().forEach(tag -> tag.setBoardId(boardId));
            }
            
            logger.info("Доска обновлена успешно: {}", savedBoard.getId());
            return Optional.of(savedBoardDTO);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении доски: {} - {}", id, e.getMessage(), e);
            return Optional.empty();
        }
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
                    dto.setProjectId(board.getProject() != null ? board.getProject().getId() : null);

                    // Преобразуем участников
                    Set<UserResponse> participants = board.getParticipants().stream()
                        .map(user -> new UserResponse(user.getUsername(), user.getName(), user.getAvatarURL()))
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
                            // Преобразуем задачи, если нужно (оставим пустым, если не требуется)
                            colDto.setTasks(Collections.emptySet());
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
                    // Делаем flush чтобы гарантировать запись изменений в БД
                    dashboardColumnRepository.flush();
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