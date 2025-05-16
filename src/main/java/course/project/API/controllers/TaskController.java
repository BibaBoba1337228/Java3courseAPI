package course.project.API.controllers;

import course.project.API.models.*;
import course.project.API.repositories.UserRepository;
import course.project.API.services.*;
import course.project.API.repositories.TagRepository;
import course.project.API.repositories.AttachmentRepository;
import course.project.API.dto.board.TaskDTO;
import course.project.API.dto.board.TagDTO;
import course.project.API.dto.board.ChecklistItemDTO;
import course.project.API.dto.board.AttachmentDTO;
import course.project.API.dto.board.TaskSearchRequest;
import course.project.API.dto.user.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.HashSet;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final ChecklistItemService checklistItemService;
    private final TagRepository tagRepository;
    private final TagService tagService;
    private final AttachmentRepository attachmentRepository;
    private final BoardRightService boardRightService;
    private final WebSocketService webSocketService;
    private final String baseUrl;
    private final BoardService boardService;
    private final ProjectService projectService;
    private final ProjectRightService projectRightService;
    private final TaskHistoryService taskHistoryService;
    private final AttachmentService attachmentService;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public TaskController(TaskService taskService, UserRepository userRepository, 
                          ChecklistItemService checklistItemService, 
                          TagRepository tagRepository,
                          TagService tagService,
                          AttachmentRepository attachmentRepository,
                          BoardRightService boardRightService,
                          WebSocketService webSocketService,
                          BoardService boardService,
                          ProjectService projectService,
                          ProjectRightService projectRightService,
                          TaskHistoryService taskHistoryService,
                          AttachmentService attachmentService,
                          PlatformTransactionManager transactionManager) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.checklistItemService = checklistItemService;
        this.tagRepository = tagRepository;
        this.tagService = tagService;
        this.attachmentRepository = attachmentRepository;
        this.boardRightService = boardRightService;
        this.webSocketService = webSocketService;
        this.baseUrl = "http://localhost:8080"; // Базовый URL для скачивания файлов
        this.boardService = boardService;
        this.projectService = projectService;
        this.projectRightService = projectRightService;
        this.taskHistoryService = taskHistoryService;
        this.attachmentService = attachmentService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // Обновляем метод convertToTaskDTO для возможности принудительного чтения вложений
    private TaskDTO convertToTaskDTO(Task task, Long boardId) {
        return convertToTaskDTO(task, boardId, false);
    }

    // Перегруженный метод с возможностью явного чтения вложений
    private TaskDTO convertToTaskDTO(Task task, Long boardId, boolean forceLoadAttachments) {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(task.getId());
        taskDTO.setTitle(task.getTitle());
        taskDTO.setDescription(task.getDescription());
        taskDTO.setColumnId(task.getColumn().getId());
        taskDTO.setStartDate(task.getStartDate());
        taskDTO.setEndDate(task.getEndDate());
        taskDTO.setPosition(task.getPosition());
        
        // Convert participants to UserResponse
        Set<UserResponse> participants = task.getParticipants().stream()
            .map(user -> new UserResponse(user.getId(), user.getName(), user.getAvatarURL()))
            .collect(Collectors.toSet());
        taskDTO.setParticipants(participants);
        
        // Convert tag to TagDTO if exists
        if (task.getTag() != null) {
            TagDTO tagDTO = new TagDTO(
                task.getTag().getId(),
                task.getTag().getName(),
                task.getTag().getColor(),
                boardId
            );
            taskDTO.setTag(tagDTO);
        }
        
        // Convert checklist items to ChecklistItemDTO
        List<ChecklistItemDTO> checklistItems = task.getChecklist().stream()
            .map(item -> new ChecklistItemDTO(
                item.getId(),
                item.getText(),
                item.isCompleted(),
                item.getPosition()
            ))
            .collect(Collectors.toList());
        taskDTO.setChecklist(checklistItems);
        
        // Если нужно принудительно получить вложения напрямую из БД
        if (forceLoadAttachments) {
            // Получаем вложения напрямую из базы
            List<Attachment> attachments = attachmentService.getAllAttachmentsByTask(task.getId());
            
            // Конвертируем в DTO и устанавливаем в объект TaskDTO
            List<AttachmentDTO> attachmentDTOs = attachments.stream()
                .map(attachment -> {
                    AttachmentDTO dto = new AttachmentDTO(
                        attachment.getId(),
                        attachment.getFileName(),
                        null, // Don't expose file path to client
                        attachment.getFileType(),
                        attachment.getFileSize(),
                        attachment.getUploadedBy(),
                        attachment.getUploadedAt()
                    );
                    // Set download URL
                    dto.setDownloadUrl(baseUrl + "/api/attachments/" + attachment.getId() + "/download");
                    return dto;
                })
                .collect(Collectors.toList());
            taskDTO.setAttachments(attachmentDTOs);
            
            // Логируем для отладки
            logger.info("Принудительно загружено {} вложений для задачи {}", 
                attachmentDTOs.size(), task.getId());
        } else {
            // Стандартный способ конвертации вложений
            List<AttachmentDTO> attachmentDTOs = task.getAttachments().stream()
                .map(attachment -> {
                    AttachmentDTO dto = new AttachmentDTO(
                        attachment.getId(),
                        attachment.getFileName(),
                        null, // Don't expose file path to client
                        attachment.getFileType(),
                        attachment.getFileSize(),
                        attachment.getUploadedBy(),
                        attachment.getUploadedAt()
                    );
                    // Set download URL
                    dto.setDownloadUrl(baseUrl + "/api/attachments/" + attachment.getId() + "/download");
                    return dto;
                })
                .collect(Collectors.toList());
            taskDTO.setAttachments(attachmentDTOs);
        }
        
        return taskDTO;
    }

    @GetMapping("/column/{columnId}")
    public ResponseEntity<List<TaskDTO>> getTasksByColumn(
            @PathVariable Long columnId,
            @AuthenticationPrincipal User currentUser) {
        
        // Get board ID for the column for permission check
        DashBoardColumn column = taskService.getColumnById(columnId);
        if (column == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = column.getBoard().getId();
        
        // Check if user has VIEW_BOARD right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD)) {
            return ResponseEntity.status(403).body(null);
        }
        
        List<Task> tasks = taskService.getAllTasksByColumn(columnId);
        
        // Convert to list of TaskDTO
        List<TaskDTO> taskDTOs = tasks.stream()
            .map(task -> convertToTaskDTO(task, boardId))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(taskDTOs);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTaskById(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {
        
        Task task = taskService.getTaskById(taskId).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = task.getColumn().getBoard().getId();
        
        // Check if user has VIEW_BOARD right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD)) {
            return ResponseEntity.status(403).body(null);
        }
        
        // Convert to TaskDTO using helper method
        TaskDTO taskDTO = convertToTaskDTO(task, boardId);
        
        return ResponseEntity.ok(taskDTO);
    }
    
    @GetMapping("/{taskId}/available-tags")
    public ResponseEntity<List<Tag>> getAvailableTagsForTask(@PathVariable Long taskId) {
        return taskService.getTaskById(taskId)
                .map(task -> {
                    Long boardId = task.getColumn().getBoard().getId();
                    List<Tag> tags = tagRepository.findByBoardId(boardId);
                    return ResponseEntity.ok(tags);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/column/{columnId}/available-tags")
    public ResponseEntity<List<Tag>> getAvailableTagsForColumn(@PathVariable Long columnId) {
        return ResponseEntity.ok(tagService.getTagsByColumnId(columnId));
    }

    /**
     * Извлекает имя пользователя из объекта участника
     * @param participant может быть строкой или объектом с полем username
     * @return имя пользователя
     */
    private String extractUsername(Object participant) {
        if (participant == null) {
            return null;
        }
        
        if (participant instanceof String) {
            return (String) participant;
        }
        
        if (participant instanceof Map) {
            Map<String, Object> userMap = (Map<String, Object>) participant;
            if (userMap.containsKey("username")) {
                return safeStringValue(userMap.get("username"));
            }
            logger.warn("Participant object does not contain username field: {}", participant);
        }
        
        return String.valueOf(participant);
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User currentUser) {
        logger.info("Received task creation payload: {}", payload);
        
        Long columnId = safeParseLong(payload.get("columnId"));
        if (columnId == null) {
            logger.error("Invalid columnId in task creation payload: {}", payload.get("columnId"));
            return ResponseEntity.badRequest().body(null);
        }
        
        // Get board ID for permission check
        DashBoardColumn column = taskService.getColumnById(columnId);
        if (column == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = column.getBoard().getId();
        
        // Check if user has CREATE_TASKS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.CREATE_TASKS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        try {
        String title = safeStringValue(payload.get("title"));
        String description = payload.containsKey("description") ? 
                safeStringValue(payload.get("description")) : "";
        Integer position = payload.containsKey("position") ? 
                safeParseInteger(payload.get("position")) : 0;
                
        // Парсим даты, если они есть
        String startDate = payload.containsKey("startDate") ? 
                safeStringValue(payload.get("startDate")) : null;
        String endDate = payload.containsKey("endDate") ? 
                safeStringValue(payload.get("endDate")) : null;
                
        Task task;
        
        // Check if we have a tag ID or tag name
        if (payload.containsKey("tagId")) {
            Long tagId = safeParseLong(payload.get("tagId"));
            if (tagId != null) {
                logger.info("Creating task with tag ID: {}", tagId);
                    task = taskService.createTaskWithTagId(title, description, columnId, currentUser.getId(), startDate, endDate, tagId);
            } else {
                logger.info("Tag ID is null or invalid, creating task without tag");
                    task = taskService.createTask(title, description, columnId, currentUser.getId(), startDate, endDate, null, boardId);
            }
        } else {
            // Парсим тег, если он есть (по имени)
            String tagName = payload.containsKey("tags") ? 
                    safeStringValue(payload.get("tags")) : null;
            
            logger.info("Processing tag: {}, boardId: {}", tagName, boardId);
            
            if (tagName != null && !tagName.isEmpty() && boardId != null) {
                logger.info("Available tags for boardId {}: {}", 
                    boardId, tagRepository.findByBoardId(boardId));
            }
            
            // Создаем задачу с использованием boardId для тегов
                task = taskService.createTask(title, description, columnId, currentUser.getId(), startDate, endDate, tagName, boardId);
        }
        
        logger.info("Created task: {}, tag: {}", task.getId(), task.getTag());
        
        // Добавляем участников, если они есть
        if (payload.containsKey("participants") && payload.get("participants") instanceof List) {
            List<Object> participants = (List<Object>) payload.get("participants");
            logger.info("Processing participants: {}", participants);

            for (Object participant : participants) {
                if (participant instanceof Number) {
                    Long userId = ((Number) participant).longValue();
                    userRepository.findById(userId).ifPresent(user -> {
                        task.addParticipant(user);
                        logger.info("Added participant by ID: {}", userId);
                    });
                } else {
                    String username = extractUsername(participant);
                    logger.info("Extracted username: {}", username);

                    if (username != null) {
                        userRepository.findByUsername(username).ifPresent(user -> {
                            task.addParticipant(user);
                            logger.info("Added participant by username: {}", username);
                        });
                    }
                }
            }
        }
        
        // Добавляем элементы чеклиста, если они есть
        if (payload.containsKey("checklist") && payload.get("checklist") instanceof List) {
            List<Object> checklistItems = (List<Object>) payload.get("checklist");
            logger.info("Processing checklist items: {}", checklistItems);
            
            for (int i = 0; i < checklistItems.size(); i++) {
                final int position_i = i;  // Make effectively final for lambda
                if (checklistItems.get(i) instanceof Map) {
                    Map<String, Object> item = (Map<String, Object>) checklistItems.get(i);
                    String text = safeStringValue(item.get("text"));
                    boolean completed = item.containsKey("completed") && 
                            Boolean.parseBoolean(safeStringValue(item.get("completed")));
                    
                    logger.info("Creating checklist item: {}, completed: {}, position: {}", text, completed, position_i);
                    ChecklistItem checklistItem = checklistItemService.createChecklistItem(task.getId(), text, position_i);
                    
                    if (completed) {
                        checklistItemService.updateChecklistItem(checklistItem.getId(), null, true, null);
                        logger.info("Updated checklist item completed status: {}", checklistItem.getId());
                    }
                } else {
                    logger.warn("Checklist item at position {} is not a map: {}", position_i, checklistItems.get(position_i));
                }
            }
        }
        
            // Process attachments (just logging, actual upload is separate)
        if (payload.containsKey("attachments") && payload.get("attachments") instanceof List) {
            List<Object> attachments = (List<Object>) payload.get("attachments");
            logger.info("Processing attachments: {}", attachments);
            
            if (!attachments.isEmpty()) {
                logger.info("Attachment data was provided in task creation, but file upload is required.");
                logger.info("To add attachments, use the API: POST /api/attachments/upload with taskId={}, file and uploadedBy", task.getId());
            }
        }
        
            // Save the task
            Task finalTask = taskService.saveAndLogTask(task);
            
            // Record task history in a separate transaction
            try {
                taskHistoryService.recordTaskCreation(currentUser, finalTask);
            } catch (Exception e) {
                logger.error("Failed to record task history: {}", e.getMessage(), e);
                // We continue even if history recording fails - this shouldn't stop task creation
            }
            
            // Convert to TaskDTO
            TaskDTO taskDTO = convertToTaskDTO(finalTask, boardId);
            
            // Notify via WebSocket
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("id", taskDTO.getId());
            notificationPayload.put("title", taskDTO.getTitle());
            notificationPayload.put("description", taskDTO.getDescription());
            notificationPayload.put("columnId", taskDTO.getColumnId());
            notificationPayload.put("startDate", taskDTO.getStartDate());
            notificationPayload.put("endDate", taskDTO.getEndDate());
            notificationPayload.put("position", taskDTO.getPosition());
            notificationPayload.put("participants", taskDTO.getParticipants());
            notificationPayload.put("tag", taskDTO.getTag());
            notificationPayload.put("checklist", taskDTO.getChecklist());
            notificationPayload.put("attachments", taskDTO.getAttachments());
            
            webSocketService.sendMessageToBoard(boardId, "TASK_CREATED", notificationPayload);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(taskDTO);
        } catch (Exception e) {
            logger.error("Error creating task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<TaskDTO> createTaskWithAttachments(
            @RequestParam("payload") String payloadStr,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal User currentUser) {
        try {
            // Convert JSON string to Map
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> payload = objectMapper.readValue(payloadStr, new TypeReference<Map<String, Object>>() {});
            
            logger.info("Received task creation payload: {}", payload);
            
            Long columnId = safeParseLong(payload.get("columnId"));
            if (columnId == null) {
                logger.error("Invalid columnId in task creation payload: {}", payload.get("columnId"));
                return ResponseEntity.badRequest().body(null);
            }
            
            // Get board ID for permission check
            DashBoardColumn column = taskService.getColumnById(columnId);
            if (column == null) {
                return ResponseEntity.notFound().build();
            }
            
            Long boardId = column.getBoard().getId();
            
            // Check if user has CREATE_TASKS right on the board
            if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.CREATE_TASKS)) {
                return ResponseEntity.status(403).body(null);
            }
            
            String title = safeStringValue(payload.get("title"));
            String description = payload.containsKey("description") ? 
                    safeStringValue(payload.get("description")) : "";
            Integer position = payload.containsKey("position") ? 
                    safeParseInteger(payload.get("position")) : 0;
                    
            // Парсим даты, если они есть
            String startDate = payload.containsKey("startDate") ? 
                    safeStringValue(payload.get("startDate")) : null;
            String endDate = payload.containsKey("endDate") ? 
                    safeStringValue(payload.get("endDate")) : null;
                    
            Task task;
            
            // Check if we have a tag ID or tag name
            if (payload.containsKey("tagId")) {
                Long tagId = safeParseLong(payload.get("tagId"));
                if (tagId != null) {
                    logger.info("Creating task with tag ID: {}", tagId);
                        task = taskService.createTaskWithTagId(title, description, columnId, currentUser.getId(), startDate, endDate, tagId);
                } else {
                    logger.info("Tag ID is null or invalid, creating task without tag");
                        task = taskService.createTask(title, description, columnId, currentUser.getId(), startDate, endDate, null, boardId);
                }
            } else {
                // Парсим тег, если он есть (по имени)
                String tagName = payload.containsKey("tags") ? 
                        safeStringValue(payload.get("tags")) : null;
                
                logger.info("Processing tag: {}, boardId: {}", tagName, boardId);
                
                if (tagName != null && !tagName.isEmpty() && boardId != null) {
                    logger.info("Available tags for boardId {}: {}", 
                        boardId, tagRepository.findByBoardId(boardId));
                }
                
                // Создаем задачу с использованием boardId для тегов
                    task = taskService.createTask(title, description, columnId, currentUser.getId(), startDate, endDate, tagName, boardId);
            }
            
            logger.info("Created task: {}, tag: {}", task.getId(), task.getTag());
            
            // Добавляем участников, если они есть
            if (payload.containsKey("participants") && payload.get("participants") instanceof List) {
                List<Object> participants = (List<Object>) payload.get("participants");
                logger.info("Processing participants: {}", participants);

                for (Object participant : participants) {
                    if (participant instanceof Number) {
                        Long userId = ((Number) participant).longValue();
                        userRepository.findById(userId).ifPresent(user -> {
                            task.addParticipant(user);
                            logger.info("Added participant by ID: {}", userId);
                        });
                    } else {
                        String username = extractUsername(participant);
                        logger.info("Extracted username: {}", username);

                        if (username != null) {
                            userRepository.findByUsername(username).ifPresent(user -> {
                                task.addParticipant(user);
                                logger.info("Added participant by username: {}", username);
                            });
                        }
                    }
                }
            }
            
            // Добавляем элементы чеклиста, если они есть
            if (payload.containsKey("checklist") && payload.get("checklist") instanceof List) {
                List<Object> checklistItems = (List<Object>) payload.get("checklist");
                logger.info("Processing checklist items: {}", checklistItems);
                
                for (int i = 0; i < checklistItems.size(); i++) {
                    final int position_i = i;  // Make effectively final for lambda
                    if (checklistItems.get(i) instanceof Map) {
                        Map<String, Object> item = (Map<String, Object>) checklistItems.get(i);
                        String text = safeStringValue(item.get("text"));
                        boolean completed = item.containsKey("completed") && 
                                          Boolean.parseBoolean(safeStringValue(item.get("completed")));
                        
                        logger.info("Creating checklist item: {}, completed: {}, position: {}", text, completed, position_i);
                        ChecklistItem checklistItem = checklistItemService.createChecklistItem(task.getId(), text, position_i);
                        
                        if (completed) {
                            checklistItemService.updateChecklistItem(checklistItem.getId(), null, true, null);
                        }
                    } else {
                        logger.warn("Checklist item at position {} is not a map: {}", position_i, checklistItems.get(position_i));
                    }
                }
            }
            
            // Save the task first to get its ID
            Task savedTask = taskService.saveAndLogTask(task);
            
            // Process file attachments
            if (files != null && !files.isEmpty()) {
                logger.info("Processing {} file attachments", files.size());
                for (MultipartFile file : files) {
                    try {
                        Attachment attachment = attachmentService.uploadAttachment(
                            savedTask.getId(),
                            file,
                            currentUser.getUsername()
                        );
                        logger.info("Added attachment: {}", attachment.getFileName());
                    } catch (IOException e) {
                        logger.error("Failed to upload attachment: {}", e.getMessage(), e);
                    }
                }
            }
            
            // Record task history in a separate transaction
            try {
                taskHistoryService.recordTaskCreation(currentUser, savedTask);
            } catch (Exception e) {
                logger.error("Failed to record task history: {}", e.getMessage(), e);
                // We continue even if history recording fails - this shouldn't stop task creation
            }
            
            // Важно: теперь используем версию с принудительной загрузкой вложений
            TaskDTO taskDTO = convertToTaskDTO(savedTask, boardId, true);
            
            // Notify via WebSocket
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("id", taskDTO.getId());
            notificationPayload.put("title", taskDTO.getTitle());
            notificationPayload.put("description", taskDTO.getDescription());
            notificationPayload.put("columnId", taskDTO.getColumnId());
            notificationPayload.put("startDate", taskDTO.getStartDate());
            notificationPayload.put("endDate", taskDTO.getEndDate());
            notificationPayload.put("position", taskDTO.getPosition());
            notificationPayload.put("participants", taskDTO.getParticipants());
            notificationPayload.put("tag", taskDTO.getTag());
            notificationPayload.put("checklist", taskDTO.getChecklist());
            notificationPayload.put("attachments", taskDTO.getAttachments());
            
            webSocketService.sendMessageToBoard(boardId, "TASK_CREATED", notificationPayload);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(taskDTO);
        } catch (Exception e) {
            logger.error("Error creating task with attachments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Get the task to check permissions
            Task task = taskService.getTaskById(taskId).orElse(null);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            Long boardId = task.getColumn().getBoard().getId();
            
            // Check if user has EDIT_TASKS right on the board
            if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.EDIT_TASKS)) {
                return ResponseEntity.status(403).body(null);
            }
            
            logger.info("Updating task with ID: {}, payload: {}", taskId, payload);
            
            // Store the original task for history comparison
            Task oldTask = taskService.cloneTask(task);
            
            // Обновляем базовую информацию о задаче
            Task updatedTask = updateTaskBasicInfo(taskId, payload, boardId, currentUser);
            if (updatedTask == null) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // Обновляем участников задачи
            try {
                updateTaskParticipants(taskId, payload, currentUser);
            } catch (Exception e) {
                logger.error("Error updating task participants: {}", e.getMessage(), e);
                // Не прерываем выполнение, продолжаем с другими обновлениями
            }
            
            // Обновляем чеклист задачи
            try {
                updateTaskChecklist(taskId, payload);
            } catch (Exception e) {
                logger.error("Error updating task checklist: {}", e.getMessage(), e);
                // Не прерываем выполнение при ошибке
            }
            
            // Get the latest version of the task after all updates
            Task latestTask = taskService.getTaskById(taskId).orElse(updatedTask);
            
            // Record task update history in a separate transaction
            try {
                taskHistoryService.recordTaskUpdate(currentUser, oldTask, latestTask);
            } catch (Exception e) {
                logger.error("Failed to record task update history: {}", e.getMessage(), e);
                // Continue even if history recording fails
            }
            
            // Convert to TaskDTO
            TaskDTO taskDTO = convertToTaskDTO(latestTask, boardId);
            
            // Convert TaskDTO to Map for WebSocket notification
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("id", taskDTO.getId());
            notificationPayload.put("title", taskDTO.getTitle());
            notificationPayload.put("description", taskDTO.getDescription());
            notificationPayload.put("columnId", taskDTO.getColumnId());
            notificationPayload.put("startDate", taskDTO.getStartDate());
            notificationPayload.put("endDate", taskDTO.getEndDate());
            notificationPayload.put("position", taskDTO.getPosition());
            notificationPayload.put("participants", taskDTO.getParticipants());
            notificationPayload.put("tag", taskDTO.getTag());
            notificationPayload.put("checklist", taskDTO.getChecklist());
            notificationPayload.put("attachments", taskDTO.getAttachments());
            notificationPayload.put("initiatedBy", currentUser.getUsername());
            
            // Add WebSocket notification
            webSocketService.sendMessageToBoard(boardId, "TASK_UPDATED", notificationPayload);
            
            return ResponseEntity.ok(taskDTO);
        } catch (Exception e) {
            logger.error("Error updating task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping(path = "/{taskId}", consumes = "multipart/form-data")
    public ResponseEntity<TaskDTO> updateTaskWithAttachments(
            @PathVariable Long taskId,
            @RequestParam("payload") String payloadStr,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal User currentUser) {
        try {
            // Convert JSON string to Map
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> payload = objectMapper.readValue(payloadStr, new TypeReference<Map<String, Object>>() {});
            
            // Get the task to check permissions
            Task task = taskService.getTaskById(taskId).orElse(null);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            
            Long boardId = task.getColumn().getBoard().getId();
            
            // Check if user has EDIT_TASKS right on the board
            if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.EDIT_TASKS)) {
                return ResponseEntity.status(403).body(null);
            }
            
            logger.info("Updating task with ID: {}, payload: {}", taskId, payload);
            
            // Store the original task for history comparison
            Task oldTask = taskService.cloneTask(task);
            
            // Обновляем базовую информацию о задаче
            Task updatedTask = updateTaskBasicInfo(taskId, payload, boardId, currentUser);
            if (updatedTask == null) {
                return ResponseEntity.badRequest().body(null);
            }
            
            // Обновляем участников задачи
            try {
                updateTaskParticipants(taskId, payload, currentUser);
            } catch (Exception e) {
                logger.error("Error updating task participants: {}", e.getMessage(), e);
                // Не прерываем выполнение, продолжаем с другими обновлениями
            }
            
            // Обновляем чеклист задачи
            try {
                updateTaskChecklist(taskId, payload);
            } catch (Exception e) {
                logger.error("Error updating task checklist: {}", e.getMessage(), e);
                // Не прерываем выполнение при ошибке
            }
            
            // Обработка обновления вложений
            try {
                updateTaskAttachments(taskId, payload, files, currentUser);
            } catch (Exception e) {
                logger.error("Error updating task attachments: {}", e.getMessage(), e);
                // Продолжаем выполнение при ошибке обновления вложений
            }
            
            // Get the latest version of the task after all updates - важно получить свежие данные из БД
            Task latestTask = taskService.getTaskById(taskId)
                .orElseThrow(() -> new RuntimeException("Failed to reload task after update"));
            
            // Record task update history in a separate transaction
            try {
                taskHistoryService.recordTaskUpdate(currentUser, oldTask, latestTask);
            } catch (Exception e) {
                logger.error("Failed to record task update history: {}", e.getMessage(), e);
                // Continue even if history recording fails
            }
            
            // Convert to TaskDTO
            TaskDTO taskDTO = convertToTaskDTO(latestTask, boardId, true);
            
            // Convert TaskDTO to Map for WebSocket notification
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("id", taskDTO.getId());
            notificationPayload.put("title", taskDTO.getTitle());
            notificationPayload.put("description", taskDTO.getDescription());
            notificationPayload.put("columnId", taskDTO.getColumnId());
            notificationPayload.put("startDate", taskDTO.getStartDate());
            notificationPayload.put("endDate", taskDTO.getEndDate());
            notificationPayload.put("position", taskDTO.getPosition());
            notificationPayload.put("participants", taskDTO.getParticipants());
            notificationPayload.put("tag", taskDTO.getTag());
            notificationPayload.put("checklist", taskDTO.getChecklist());
            notificationPayload.put("attachments", taskDTO.getAttachments());
            notificationPayload.put("initiatedBy", currentUser.getUsername());
            
            // Add WebSocket notification
            webSocketService.sendMessageToBoard(boardId, "TASK_UPDATED", notificationPayload);
            
            return ResponseEntity.ok(taskDTO);
        } catch (Exception e) {
            logger.error("Error updating task with attachments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновляет основную информацию о задаче (название, описание, позиция, колонка, метка, даты)
     */
    @Transactional
    protected Task updateTaskBasicInfo(Long taskId, Map<String, Object> payload, Long boardId, User currentUser) {
        try {
            String title = payload.containsKey("title") ? safeStringValue(payload.get("title")) : null;
            String description = payload.containsKey("description") ? safeStringValue(payload.get("description")) : null;
            Integer position = payload.containsKey("position") ? safeParseInteger(payload.get("position")) : null;
            Long columnId = payload.containsKey("columnId") ? safeParseLong(payload.get("columnId")) : null;
            String startDate = payload.containsKey("startDate") ? safeStringValue(payload.get("startDate")) : null;
            String endDate = payload.containsKey("endDate") ? safeStringValue(payload.get("endDate")) : null;

            // Check if moving to a different column
            Task task = taskService.getTaskById(taskId).orElse(null);
            if (task == null) {
                return null;
            }
            
            if (columnId != null && !columnId.equals(task.getColumn().getId())) {
                // Check if user has MOVE_TASKS right
                if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_TASKS)) {
                    throw new RuntimeException("User doesn't have permission to move tasks");
                }
            }
            
            String tagName = payload.containsKey("tags") ? safeStringValue(payload.get("tags")) : null;
            Long boardIdFromPayload = null;
            if (payload.containsKey("boardId")) {
                boardIdFromPayload = safeParseLong(payload.get("boardId"));
            } else if (payload.containsKey("projectId")) {
                boardIdFromPayload = safeParseLong(payload.get("projectId"));
            }
            
            Task updatedTask;
            if (payload.containsKey("tagId")) {
                Long tagId = safeParseLong(payload.get("tagId"));
                logger.info("Updating task with tag ID: {}", tagId);
                updatedTask = taskService.updateTaskWithTagId(
                    taskId, title, description, position, columnId, tagId, startDate, endDate);
            } else {
                logger.info("Updating tag with name: {}, boardId: {}", tagName, boardIdFromPayload);
                updatedTask = taskService.updateTask(
                    taskId, title, description, position, columnId, tagName, startDate, endDate, boardIdFromPayload);
            }
            
            return updatedTask;
        } catch (Exception e) {
            logger.error("Error updating task basic info: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Обновляет список участников задачи
     */
    @Transactional
    protected void updateTaskParticipants(Long taskId, Map<String, Object> payload, User currentUser) {
        if (!payload.containsKey("participants") || !(payload.get("participants") instanceof List)) {
            return;
        }
        
        List<Object> participants = (List<Object>) payload.get("participants");
        logger.info("Processing participants update: {}", participants);
        
        // Get current participants
        Set<User> currentParticipants = taskService.getTaskParticipants(taskId);
        logger.info("Current participants: {}", currentParticipants.stream().map(User::getId).collect(Collectors.toList()));
        
        List<Long> newParticipantIds = new ArrayList<>();
        List<String> newParticipantUsernames = new ArrayList<>();
        
        // Process new participants list - добавляем новых участников
        for (Object participant : participants) {
            if (participant instanceof Number) {
                // If participant is a number (ID)
                Long userId = ((Number) participant).longValue();
                newParticipantIds.add(userId);
                userRepository.findById(userId).ifPresent(user -> {
                    if (!currentParticipants.contains(user)) {
                        try {
                            taskService.addParticipantToTask(taskId, user.getId());
                            logger.info("Added participant by ID: {}", userId);
                        } catch (Exception e) {
                            logger.error("Error adding participant ID {}: {}", userId, e.getMessage(), e);
                        }
                    }
                });
            } else {
                // If participant is a string or object with username
                String username = extractUsername(participant);
                if (username != null) {
                    newParticipantUsernames.add(username);
                    // Add new participants
                    userRepository.findByUsername(username).ifPresent(user -> {
                        if (!currentParticipants.contains(user)) {
                            try {
                                taskService.addParticipantToTask(taskId, user.getId());
                                logger.info("Added participant by username: {}", username);
                            } catch (Exception e) {
                                logger.error("Error adding participant by username {}: {}", username, e.getMessage(), e);
                            }
                        }
                    });
                }
            }
        }
        
        logger.info("New participant IDs: {}", newParticipantIds);
        logger.info("New participant usernames: {}", newParticipantUsernames);
        
        // Remove participants that are no longer in the list - удаляем отсутствующих участников
        for (User currentParticipant : new ArrayList<>(currentParticipants)) {
            boolean shouldKeep = newParticipantIds.contains(currentParticipant.getId()) || 
                                newParticipantUsernames.contains(currentParticipant.getUsername());
            
            logger.info("Checking if should keep participant ID {}: {}", currentParticipant.getId(), shouldKeep);
            
            if (!shouldKeep) {
                try {
                    taskService.removeParticipantFromTask(taskId, currentParticipant.getId());
                    logger.info("Removed participant: {} (ID: {})", currentParticipant.getUsername(), currentParticipant.getId());
                } catch (Exception e) {
                    logger.error("Error removing participant ID {}: {}", currentParticipant.getId(), e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Обновляет чеклист задачи - полностью заменяет старый чеклист новым
     */
    @Transactional
    protected void updateTaskChecklist(Long taskId, Map<String, Object> payload) {
        if (!payload.containsKey("checklist")) {
            return; // Чеклист не меняется, если его нет в запросе
        }
        
        try {
            logger.info("Replacing checklist for task ID: {}", taskId);
            
            // Получаем текущую задачу
            Task currentTask = taskService.getTaskById(taskId).orElse(null);
            if (currentTask == null) {
                logger.error("Task not found with ID: {}", taskId);
                return;
            }
            
            // Очищаем существующий чеклист напрямую через коллекцию задачи
            List<ChecklistItem> existingItems = new ArrayList<>(currentTask.getChecklist());
            for (ChecklistItem item : existingItems) {
                currentTask.removeChecklistItem(item);
                checklistItemService.deleteChecklistItem(item.getId());
            }
            
            // Сохраняем задачу с пустым чеклистом
            taskService.saveAndLogTask(currentTask);
            
            // Если пришел пустой чеклист, просто завершаем (чеклист уже пустой)
            if (payload.get("checklist") == null || 
                "".equals(payload.get("checklist")) || 
                (payload.get("checklist") instanceof List && ((List)payload.get("checklist")).isEmpty())) {
                logger.info("Received empty checklist, all items deleted");
                return;
            }
            
            // Добавляем новые элементы из запроса
            if (payload.get("checklist") instanceof List) {
                List<Object> checklistItems = (List<Object>) payload.get("checklist");
                logger.info("Adding {} new checklist items", checklistItems.size());
                
                for (int i = 0; i < checklistItems.size(); i++) {
                    if (checklistItems.get(i) instanceof Map) {
                        Map<String, Object> item = (Map<String, Object>) checklistItems.get(i);
                        String text = safeStringValue(item.get("text"));
                        boolean completed = item.containsKey("completed") && 
                                          Boolean.parseBoolean(safeStringValue(item.get("completed")));
                        
                        // Создаем новые элементы и добавляем их к задаче
                        ChecklistItem checklistItem = checklistItemService.createChecklistItem(taskId, text, i);
                        if (completed) {
                            checklistItemService.updateChecklistItem(checklistItem.getId(), null, true, null);
                        }
                        logger.info("Created new checklist item: {}", checklistItem.getId());
                    }
                }
            } else {
                logger.warn("Unexpected checklist format: {}", payload.get("checklist"));
            }
        } catch (Exception e) {
            logger.error("Error replacing checklist: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Обновляет вложения задачи - обрабатывает как новые файлы, так и удаление старых
     */
    @Transactional
    protected void updateTaskAttachments(
            Long taskId, 
            Map<String, Object> payload, 
            List<MultipartFile> files, 
            User currentUser) throws IOException {
        
        // Обработать список существующих вложений, которые нужно сохранить
        Set<Long> attachmentsToKeep = new HashSet<>();
        
        if (payload.containsKey("attachments") && payload.get("attachments") instanceof List) {
            List<Object> attachmentsList = (List<Object>) payload.get("attachments");
            
            for (Object attachmentObj : attachmentsList) {
                if (attachmentObj instanceof Map) {
                    Map<String, Object> attachmentMap = (Map<String, Object>) attachmentObj;
                    if (attachmentMap.containsKey("id")) {
                        Long attachmentId = safeParseLong(attachmentMap.get("id"));
                        if (attachmentId != null) {
                            attachmentsToKeep.add(attachmentId);
                        }
                    }
                } else if (attachmentObj instanceof Number) {
                    Long attachmentId = ((Number) attachmentObj).longValue();
                    attachmentsToKeep.add(attachmentId);
                }
            }
        }
        
        // Получить текущие вложения
        List<Attachment> existingAttachments = attachmentService.getAllAttachmentsByTask(taskId);
        
        // Удалить вложения, которых нет в списке для сохранения
        for (Attachment attachment : existingAttachments) {
            if (!attachmentsToKeep.contains(attachment.getId())) {
                logger.info("Deleting attachment: {}", attachment.getId());
                attachmentService.deleteAttachment(attachment.getId());
            }
        }
        
        // Добавить новые вложения, если они есть
        if (files != null && !files.isEmpty()) {
            logger.info("Processing {} new file attachments", files.size());
            for (MultipartFile file : files) {
                try {
                    Attachment attachment = attachmentService.uploadAttachment(
                        taskId,
                        file,
                        currentUser.getUsername()
                    );
                    logger.info("Added new attachment: {}", attachment.getFileName());
                } catch (IOException e) {
                    logger.error("Failed to upload attachment: {}", e.getMessage(), e);
                    throw e;
                }
            }
        }
    }

    private String safeStringValue(Object obj) {
        if (obj == null) {
            return "";
        }
        return String.valueOf(obj);
    }

    // Добавляем метод для безопасного парсинга числовых значений
    private Long safeParseLong(Object value) {
        if (value == null) {
            return null;
        }
        String strValue = String.valueOf(value);
        if (strValue.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(strValue);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse value to Long: {}", value);
            return null;
        }
    }

    private Integer safeParseInteger(Object value) {
        if (value == null) {
            return null;
        }
        String strValue = String.valueOf(value);
        if (strValue.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(strValue);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse value to Integer: {}", value);
            return null;
        }
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {
        
        try {
        // Get the task to check permissions
        Task task = taskService.getTaskById(taskId).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = task.getColumn().getBoard().getId();
        
        // Check if user has DELETE_TASKS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.DELETE_TASKS)) {
            return ResponseEntity.status(403).build();
        }
        
            // Record task deletion history before deleting the task in a separate transaction
            try {
                taskHistoryService.recordTaskDeletion(currentUser, task);
            } catch (Exception e) {
                logger.error("Failed to record task deletion history: {}", e.getMessage(), e);
                // Continue even if history recording fails
            }
            
            // Delete the task (история задачи будет сохранена в базе данных)
        taskService.deleteTask(taskId);
        
        // Add WebSocket notification
        Map<String, Object> notificationPayload = new HashMap<>();
        notificationPayload.put("taskId", taskId);
        notificationPayload.put("initiatedBy", currentUser.getUsername());
        webSocketService.sendMessageToBoard(boardId, "TASK_DELETED", notificationPayload);
        
        return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/column/{columnId}")
    public ResponseEntity<Void> deleteAllTasksByColumn(
            @PathVariable Long columnId,
            @AuthenticationPrincipal User currentUser) {
        
        // Get board ID for permission check
        DashBoardColumn column = taskService.getColumnById(columnId);
        if (column == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = column.getBoard().getId();
        
        // Check if user has DELETE_TASKS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.DELETE_TASKS)) {
            return ResponseEntity.status(403).build();
        }
        
        taskService.deleteAllTasksByColumn(columnId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderTasks(
            @RequestBody List<Long> taskIds,
            @AuthenticationPrincipal User currentUser) {
        
        // Get first task to check permissions
        if (taskIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Task task = taskService.getTaskById(taskIds.get(0)).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = task.getColumn().getBoard().getId();
        
        // Check if user has MOVE_TASKS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_TASKS)) {
            return ResponseEntity.status(403).build();
        }
        
        taskService.updateTasksPositions(taskIds);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{taskId}/move")
    public ResponseEntity<Task> moveTaskToColumn(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User currentUser) {
        
        // Get the task to check permissions
        Task task = taskService.getTaskById(taskId).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = task.getColumn().getBoard().getId();
        
        // Check if user has MOVE_TASKS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_TASKS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        // Get target column ID (where we're moving the task to)
        Long targetColumnId = safeParseLong(payload.get("targetColumnId"));
        if (targetColumnId == null) {
            // Fallback to columnId if targetColumnId not provided
            targetColumnId = safeParseLong(payload.get("columnId"));
            if (targetColumnId == null) {
                logger.error("Missing or invalid targetColumnId or columnId in payload: {}", payload);
                return ResponseEntity.badRequest().build();
            }
        }
        
        // Get position in the target column
        Integer position = safeParseInteger(payload.get("newPosition"));
        if (position == null) {
            position = safeParseInteger(payload.get("position"));
            if (position == null) {
                logger.error("Missing or invalid newPosition or position in payload: {}", payload);
                return ResponseEntity.badRequest().build();
            }
        }
        
        logger.info("Moving task {} to column {} at position {}", taskId, targetColumnId, position);
        Task movedTask = taskService.moveTaskToColumn(taskId, targetColumnId, position);
        if (movedTask != null) {
            // Add WebSocket notification
            Map<String, Object> notificationPayload = new HashMap<>(payload);
            notificationPayload.put("taskId", taskId);
            notificationPayload.put("initiatedBy", currentUser.getUsername());
            webSocketService.sendMessageToBoard(boardId, "TASK_MOVED", notificationPayload);
            
            return ResponseEntity.ok(movedTask);
        }
        
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{taskId}/participants/{userId}")
    public ResponseEntity<Task> addParticipantToTask(
            @PathVariable Long taskId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Get the task to check permissions
        Task task = taskService.getTaskById(taskId).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = task.getColumn().getBoard().getId();
        
        // Check if user has EDIT_TASKS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.EDIT_TASKS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        Task updatedTask = taskService.addParticipantToTask(taskId, userId);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{taskId}/participants/{userId}")
    public ResponseEntity<Task> removeParticipantFromTask(
            @PathVariable Long taskId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        // Get the task to check permissions
        Task task = taskService.getTaskById(taskId).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = task.getColumn().getBoard().getId();
        
        // Check if user has EDIT_TASKS right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.EDIT_TASKS)) {
            return ResponseEntity.status(403).body(null);
        }
        
        Task updatedTask = taskService.removeParticipantFromTask(taskId, userId);
        return ResponseEntity.ok(updatedTask);
    }

    @GetMapping("/{taskId}/participants")
    public ResponseEntity<Set<User>> getTaskParticipants(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {
        
        // Get the task to check permissions
        Task task = taskService.getTaskById(taskId).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = task.getColumn().getBoard().getId();
        
        // Check if user has VIEW_BOARD right on the board
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD)) {
            return ResponseEntity.status(403).body(null);
        }
        
        return ResponseEntity.ok(taskService.getTaskParticipants(taskId));
    }

    @PutMapping("/column/{columnId}/reorder")
    public ResponseEntity<Void> reorderTasksInColumn(
            @PathVariable Long columnId,
            @RequestBody List<Long> taskIds,
            @AuthenticationPrincipal User currentUser) {
        
        // Get board ID for permission check
        DashBoardColumn column = taskService.getColumnById(columnId);
        if (column == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = column.getBoard().getId();
        
        // Check if user has MOVE_TASKS right
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.MOVE_TASKS)) {
            return ResponseEntity.status(403).build();
        }
        
        // Verify all tasks belong to this column
        for (Long taskId : taskIds) {
            Task task = taskService.getTaskById(taskId).orElse(null);
            if (task == null || !task.getColumn().getId().equals(columnId)) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        taskService.updateTasksPositions(taskIds);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Получает все задачи, в которых текущий пользователь является участником
     * @param authentication данные аутентификации текущего пользователя
     * @return список всех задач пользователя
     */
    @GetMapping("/my")
    public ResponseEntity<List<Task>> getMyTasks(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(taskService.getUserTasksById(user.getId()));
    }

    /**
     * Получить все задачи пользователя в проекте
     * 
     * @param projectId ID проекта
     * @param currentUser текущий пользователь
     * @return список всех задач пользователя в проекте
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getUserTasksByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // Проверяем, что пользователь имеет доступ к проекту
            if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.VIEW_PROJECT)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "У вас нет доступа к этому проекту");
                return ResponseEntity.status(403).body(error);
            }
            
            List<TaskDTO> tasks = boardService.getUserTasksByProjectId(projectId, currentUser.getId());
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/search")
    public ResponseEntity<List<TaskDTO>> searchTasks(
            @RequestBody TaskSearchRequest searchRequest,
            @AuthenticationPrincipal User currentUser) {
        
        logger.info("Searching tasks with criteria: {}", searchRequest);
        
        // Perform the search
        List<Task> tasks = taskService.searchTasks(
            searchRequest.getSearchText(),
            searchRequest.getProjectId(),
            searchRequest.getBoardId(),
            searchRequest.getTagId(),
            searchRequest.getIsCompleted(),
            searchRequest.getParticipantIds(),
            searchRequest.getSortDirection()
        );
        
        // Convert tasks to DTOs
        List<TaskDTO> taskDTOs = tasks.stream()
            .map(task -> {
                Long boardId = task.getColumn().getBoard().getId();
                return convertToTaskDTO(task, boardId, true);
            })
            .collect(Collectors.toList());
        
        logger.info("Returning {} tasks from search", taskDTOs.size());
        return ResponseEntity.ok(taskDTOs);
    }
} 