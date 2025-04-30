package course.project.API.controllers;

import course.project.API.dto.board.TagDTO;
import course.project.API.models.ChecklistItem;
import course.project.API.models.Tag;
import course.project.API.models.Task;
import course.project.API.models.User;
import course.project.API.repositories.UserRepository;
import course.project.API.services.ChecklistItemService;
import course.project.API.services.TaskService;
import course.project.API.services.TagService;
import course.project.API.services.PermissionService;
import course.project.API.repositories.TagRepository;
import course.project.API.repositories.AttachmentRepository;
import course.project.API.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;

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
    private final PermissionService permissionService;
    private final TaskRepository taskRepository;

    @Autowired
    public TaskController(TaskService taskService, 
                         UserRepository userRepository, 
                         ChecklistItemService checklistItemService, 
                         TagRepository tagRepository,
                         TagService tagService,
                         AttachmentRepository attachmentRepository,
                         PermissionService permissionService,
                         TaskRepository taskRepository) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.checklistItemService = checklistItemService;
        this.tagRepository = tagRepository;
        this.tagService = tagService;
        this.attachmentRepository = attachmentRepository;
        this.permissionService = permissionService;
        this.taskRepository = taskRepository;
    }

    @GetMapping("/column/{columnId}")
    public ResponseEntity<List<Task>> getTasksByColumn(
            @PathVariable Long columnId,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasBoardAccess(user.getId(), columnId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(taskService.getAllTasksByColumn(columnId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(
            @PathVariable Long taskId,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasTaskAccess(user.getId(), taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return taskService.getTaskById(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{taskId}/available-tags")
    public ResponseEntity<List<Tag>> getAvailableTagsForTask(
            @PathVariable Long taskId,
            Principal principal) {
            
        var currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasTaskAccess(currentUser.getId(), taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return taskService.getTaskById(taskId)
                .map(task -> {
                    Long boardId = task.getColumn().getBoard().getId();
                    List<Tag> tags = tagRepository.findByBoardId(boardId);
                    return ResponseEntity.ok(tags);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/column/{columnId}/available-tags")
    public ResponseEntity<List<Tag>> getAvailableTagsForColumn(
            @PathVariable Long columnId,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasBoardAccess(user.getId(), columnId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
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
    public ResponseEntity<Task> createTask(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        Long columnId = Long.parseLong(safeStringValue(payload.get("columnId")));
        Long boardId = null;
        
        if (payload.containsKey("boardId")) {
            boardId = Long.parseLong(safeStringValue(payload.get("boardId")));
        } else if (payload.containsKey("projectId")) {
            boardId = Long.parseLong(safeStringValue(payload.get("projectId")));
        }
        
        if (!permissionService.hasBoardAccess(user.getId(), boardId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        logger.info("Using boardId: {}", boardId);
        
        String title = safeStringValue(payload.get("title"));
        String description = payload.containsKey("description") ? 
                safeStringValue(payload.get("description")) : "";
        Integer position = payload.containsKey("position") ? 
                Integer.parseInt(safeStringValue(payload.get("position"))) : 0;
                
        // Парсим даты, если они есть
        String startDate = payload.containsKey("startDate") ? 
                safeStringValue(payload.get("startDate")) : null;
        String endDate = payload.containsKey("endDate") ? 
                safeStringValue(payload.get("endDate")) : null;
                
        Task task;
        
        // Check if we have a tag ID or tag name
        if (payload.containsKey("tagId")) {
            Long tagId = Long.parseLong(safeStringValue(payload.get("tagId")));
            logger.info("Creating task with tag ID: {}", tagId);
            task = taskService.createTaskWithTagId(title, description, columnId, null, startDate, endDate, tagId);
        } else {
            // Парсим тег, если он есть (по имени)
            String tagName = payload.containsKey("tags") ? 
                    safeStringValue(payload.get("tags")) : null;
            
            logger.info("Processing tag: {}, boardId: {}", tagName, boardId);
            
            if (tagName != null && boardId != null) {
                logger.info("Available tags for boardId {}: {}", 
                    boardId, tagRepository.findByBoardId(boardId));
            }
            
            // Создаем задачу с использованием boardId для тегов
            task = taskService.createTask(title, description, columnId, null, startDate, endDate, tagName, boardId);
        }
        
        logger.info("Created task: {}, tag: {}", task.getId(), task.getTag());
        
        // Добавляем участников, если они есть
        if (payload.containsKey("participants") && payload.get("participants") instanceof List) {
            List<Object> participants = (List<Object>) payload.get("participants");
            logger.info("Processing participants: {}", participants);
            
            for (Object participant : participants) {
                String username = extractUsername(participant);
                logger.info("Extracted username: {}", username);
                
                if (username != null) {
                    userRepository.findByUsername(username).ifPresent(foundUser -> {
                        taskService.addParticipantToTask(task.getId(), foundUser.getId());
                        logger.info("Added participant: {}", username);
                    });
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
        
        // Перезагружаем задачу, чтобы получить полные данные
        Task refreshedTask = taskService.getTaskById(task.getId())
                .orElse(task);
        
        logger.info("Returning refreshed task: {}, tag: {}, checklist size: {}", 
            refreshedTask.getId(), refreshedTask.getTag(), refreshedTask.getChecklist().size());
                
        return ResponseEntity.status(HttpStatus.CREATED).body(refreshedTask);
    }

    @PutMapping("/{taskId}")
    @Transactional
    public ResponseEntity<Task> updateTask(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageTask(user.getId(), taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        logger.info("Updating task with ID: {}, payload: {}", taskId, payload);
        
        String title = payload.containsKey("title") ? safeStringValue(payload.get("title")) : null;
        String description = payload.containsKey("description") ? safeStringValue(payload.get("description")) : null;
        Integer position = payload.containsKey("position") ? Integer.parseInt(safeStringValue(payload.get("position"))) : null;
        Long columnId = payload.containsKey("columnId") ? Long.parseLong(safeStringValue(payload.get("columnId"))) : null;
        String startDate = payload.containsKey("startDate") ? safeStringValue(payload.get("startDate")) : null;
        String endDate = payload.containsKey("endDate") ? safeStringValue(payload.get("endDate")) : null;

        // Board ID can come as boardId or projectId in the payload
        Long boardId = null;
        if (payload.containsKey("boardId")) {
            boardId = Long.parseLong(safeStringValue(payload.get("boardId")));
        } else if (payload.containsKey("projectId")) {
            boardId = Long.parseLong(safeStringValue(payload.get("projectId")));
        }

        Task updatedTask;
        if (payload.containsKey("tagId")) {
            Long tagId = Long.parseLong(safeStringValue(payload.get("tagId")));
            logger.info("Updating task with tag ID: {}", tagId);
            updatedTask = taskService.updateTaskWithTagId(
                taskId, title, description, position, columnId, tagId, startDate, endDate);
        } else {
            String tagName = payload.containsKey("tags") ? safeStringValue(payload.get("tags")) : null;
            logger.info("Updating tag with name: {}, boardId: {}", tagName, boardId);
            updatedTask = taskService.updateTask(
                taskId, title, description, position, columnId, tagName, startDate, endDate, boardId);
        }

        // --- Обновление участников ---
        if (payload.containsKey("participants") && payload.get("participants") instanceof List) {
            List<Object> participants = (List<Object>) payload.get("participants");
            Set<User> users = new java.util.HashSet<>();
            for (Object participant : participants) {
                String username = extractUsername(participant);
                if (username != null) {
                    userRepository.findByUsername(username).ifPresent(users::add);
                }
            }
            updatedTask.setParticipants(users);
        }

        // --- Обновление чеклиста ---
        if (payload.containsKey("checklist") && payload.get("checklist") instanceof List) {
            List<Object> checklistItems = (List<Object>) payload.get("checklist");
            // Собираем id новых элементов (если есть)
            List<Long> newIds = checklistItems.stream()
                .map(itemObj -> {
                    if (itemObj instanceof Map) {
                        Map<String, Object> item = (Map<String, Object>) itemObj;
                        Object idObj = item.get("id");
                        if (idObj != null) {
                            try { return Long.parseLong(idObj.toString()); } catch (Exception e) { return null; }
                        }
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

            // Удаляем старые, которых нет в новом списке
            List<ChecklistItem> toRemove = new ArrayList<>();
            for (ChecklistItem oldItem : new ArrayList<>(updatedTask.getChecklist())) {
                if (oldItem.getId() == null || !newIds.contains(oldItem.getId())) {
                    toRemove.add(oldItem);
                }
            }
            for (ChecklistItem item : toRemove) {
                updatedTask.removeChecklistItem(item);
            }

            // Обновляем существующие и добавляем новые
            for (Object itemObj : checklistItems) {
                if (itemObj instanceof Map) {
                    Map<String, Object> item = (Map<String, Object>) itemObj;
                    String text = safeStringValue(item.get("text"));
                    boolean completed = item.containsKey("completed") && Boolean.parseBoolean(safeStringValue(item.get("completed")));
                    Long id = null;
                    if (item.get("id") != null) {
                        try { id = Long.parseLong(item.get("id").toString()); } catch (Exception e) { id = null; }
                    }
                    ChecklistItem checklistItem = null;
                    if (id != null) {
                        for (ChecklistItem old : updatedTask.getChecklist()) {
                            if (id.equals(old.getId())) {
                                checklistItem = old;
                                break;
                            }
                        }
                    }
                    if (checklistItem == null) {
                        checklistItem = new ChecklistItem(text, updatedTask);
                        checklistItem.setCompleted(completed);
                        updatedTask.addChecklistItem(checklistItem);
                    } else {
                        checklistItem.setText(text);
                        checklistItem.setCompleted(completed);
                    }
                }
            }
        }

        // --- Обновление вложений (attachments) ---
        if (payload.containsKey("attachments") && payload.get("attachments") instanceof List) {
            List<Object> attachmentIds = (List<Object>) payload.get("attachments");
            List<Long> newIds = attachmentIds.stream()
                .map(idObj -> {
                    try { return Long.parseLong(idObj.toString()); } catch (Exception e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .toList();

            // attachments, которые должны остаться
            List<course.project.API.models.Attachment> newAttachments = new java.util.ArrayList<>();
            for (Long attId : newIds) {
                attachmentRepository.findById(attId).ifPresent(newAttachments::add);
            }

            // attachments, которые были раньше, но их нет в новом списке — удалить
            java.util.List<course.project.API.models.Attachment> toRemove = new java.util.ArrayList<>();
            for (course.project.API.models.Attachment oldAtt : new java.util.ArrayList<>(updatedTask.getAttachments())) {
                if (!newIds.contains(oldAtt.getId())) {
                    toRemove.add(oldAtt);
                }
            }
            for (course.project.API.models.Attachment att : toRemove) {
                updatedTask.removeAttachment(att);
            }

            // добавить новые, которых не было
            for (course.project.API.models.Attachment att : newAttachments) {
                if (!updatedTask.getAttachments().contains(att)) {
                    updatedTask.addAttachment(att);
                }
            }
        }

        // --- Сохраняем задачу с обновлёнными участниками и чеклистом ---
        updatedTask = taskService.saveAndLogTask(updatedTask);

        return ResponseEntity.ok(updatedTask);
    }

    private String safeStringValue(Object obj) {
        if (obj == null) {
            return "";
        }
        return String.valueOf(obj);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageTask(user.getId(), taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/column/{columnId}")
    public ResponseEntity<Void> deleteAllTasksByColumn(
            @PathVariable Long columnId,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageBoard(user.getId(), columnId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        taskService.deleteAllTasksByColumn(columnId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderTasks(
            @RequestBody List<Long> taskIds,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        // Проверяем доступ к первой задаче (предполагаем, что все задачи в одной колонке)
        if (!taskIds.isEmpty() && !permissionService.canManageTask(user.getId(), taskIds.get(0))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        taskService.updateTasksPositions(taskIds);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{taskId}/move")
    public ResponseEntity<Task> moveTaskToColumn(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageTask(user.getId(), taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Long columnId = Long.parseLong(safeStringValue(payload.get("columnId")));
        Integer position = Integer.parseInt(safeStringValue(payload.get("position")));
        
        Task movedTask = taskService.moveTaskToColumn(taskId, columnId, position);
        return ResponseEntity.ok(movedTask);
    }

    @PostMapping("/{taskId}/participants/{userId}")
    public ResponseEntity<Task> addParticipantToTask(
            @PathVariable Long taskId,
            @PathVariable Long userId,
            Principal principal) {
            
        User currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageTask(currentUser.getId(), taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Task task = taskService.getTaskById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
            
        User participant = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Participant not found"));
            
        task.getParticipants().add(participant);
        taskRepository.save(task);
        
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{taskId}/participants/{userId}")
    public ResponseEntity<Task> removeParticipantFromTask(
            @PathVariable Long taskId,
            @PathVariable Long userId,
            Principal principal) {
            
        var currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageTask(currentUser.getId(), taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Task task = taskService.removeParticipantFromTask(taskId, userId);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/{taskId}/participants")
    public ResponseEntity<List<User>> getTaskParticipants(
            @PathVariable Long taskId,
            Principal principal) {
            
        User currentUser = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.hasTaskAccess(currentUser.getId(), taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Task task = taskService.getTaskById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
            
        List<User> participants = task.getParticipants().stream()
            .map(p -> {
                User participantUser = new User();
                participantUser.setId(p.getId());
                participantUser.setUsername(p.getUsername());
                return participantUser;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(participants);
    }

    @PutMapping("/column/{columnId}/reorder")
    public ResponseEntity<Void> reorderTasksInColumn(
            @PathVariable Long columnId, 
            @RequestBody List<Long> taskIds,
            Principal principal) {
            
        var user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (!permissionService.canManageBoard(user.getId(), columnId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        taskService.updateTasksPositions(taskIds);
        return ResponseEntity.ok().build();
    }
} 