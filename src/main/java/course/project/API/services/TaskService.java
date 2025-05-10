package course.project.API.services;

import course.project.API.models.*;
import course.project.API.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final DashBoardColumnRepository columnRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    public final AttachmentRepository attachmentRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository, 
                      DashBoardColumnRepository columnRepository,
                      UserRepository userRepository,
                      TagRepository tagRepository,
                      AttachmentRepository attachmentRepository) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasksByColumn(Long columnId) {
        return taskRepository.findByColumnId(columnId);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByColumnOrdered(DashBoardColumn column) {
        return taskRepository.findByColumnOrderByPosition(column);
    }

    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(Long taskId) {
        return taskRepository.findById(taskId);
    }

    @Transactional
    public Task createTask(String title, String description, Long columnId, Long assigneeId, 
                         String startDate, String endDate, String tagName) {
        return createTask(title, description, columnId, assigneeId, startDate, endDate, tagName, null);
    }

    @Transactional
    public Task createTask(String title, String description, Long columnId, Long assigneeId, 
                          String startDate, String endDate, String tagName, Long boardId) {
        DashBoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Column not found with id: " + columnId));

        Task task = new Task(title, description, column);
        logger.info("Creating task with title: {}, in column: {}", title, columnId);
        
        if (assigneeId != null) {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + assigneeId));
            task.addParticipant(assignee);
        }
        
        if (startDate != null && !startDate.isEmpty()) {
            task.setStartDate(LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME));
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            task.setEndDate(LocalDateTime.parse(endDate, DateTimeFormatter.ISO_DATE_TIME));
        }
        
        if (tagName != null && !tagName.isEmpty()) {
            logger.info("Looking for tag with name: {}", tagName);
            
            // Try first with provided boardId
            if (boardId != null) {
                logger.info("Searching for tag using explicit boardId: {}", boardId);
                Optional<Tag> tagByExplicitBoardId = tagRepository.findByNameAndBoardId(tagName, boardId);
                if (tagByExplicitBoardId.isPresent()) {
                    logger.info("Found tag by explicit boardId: {}", tagByExplicitBoardId.get().getId());
                    task.setTag(tagByExplicitBoardId.get());
                    return saveAndLogTask(task);
                }
            }
            
            // Try with board from column
            Long columnBoardId = column.getBoard().getId();
            logger.info("Board ID from column: {}", columnBoardId);
            
            // Search by name and board ID
            Optional<Tag> tagByColumnBoardId = tagRepository.findByNameAndBoardId(tagName, columnBoardId);
            
            if (tagByColumnBoardId.isPresent()) {
                logger.info("Found tag by column's boardId: {}", tagByColumnBoardId.get().getId());
                task.setTag(tagByColumnBoardId.get());
            } else {
                // Fallback to find by just name
                Optional<Tag> tagByName = tagRepository.findByName(tagName);
                if (tagByName.isPresent()) {
                    logger.info("Found tag by name only: {}", tagByName.get().getId());
                    task.setTag(tagByName.get());
                } else {
                    logger.warn("No tag found with name: {} for board: {}", tagName, columnBoardId);
                    // No matching tag found, create a new one for the board
                    Tag newTag = new Tag(tagName, generateRandomColor(), column.getBoard());
                    column.getBoard().addTag(newTag);
                    tagRepository.save(newTag);
                    task.setTag(newTag);
                    logger.info("Created new tag: {}", newTag.getId());
                }
            }
        }
        
        // Set position to last in the column
        List<Task> tasks = taskRepository.findByColumnOrderByPosition(column);
        task.setPosition(tasks.size());
        
        return saveAndLogTask(task);
    }

    @Transactional
    public Task createTaskWithTagId(String title, String description, Long columnId, Long assigneeId, 
                                   String startDate, String endDate, Long tagId) {
        DashBoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Column not found with id: " + columnId));

        Task task = new Task(title, description, column);
        logger.info("Creating task with title: {}, in column: {}, tagId: {}", title, columnId, tagId);
        
        if (assigneeId != null) {
            User assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + assigneeId));
            task.addParticipant(assignee);
        }
        
        if (startDate != null && !startDate.isEmpty()) {
            task.setStartDate(LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME));
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            task.setEndDate(LocalDateTime.parse(endDate, DateTimeFormatter.ISO_DATE_TIME));
        }
        
        if (tagId != null) {
            tagRepository.findById(tagId).ifPresent(tag -> {
                logger.info("Found tag with id: {}", tagId);
                task.setTag(tag);
            });
        }
        
        // Set position to last in the column
        List<Task> tasks = taskRepository.findByColumnOrderByPosition(column);
        task.setPosition(tasks.size());
        
        return saveAndLogTask(task);
    }

    @Transactional
    public Task updateTask(Long taskId, String title, String description, Integer position, 
                         Long columnId, String tagName, String startDate, String endDate) {
        return updateTask(taskId, title, description, position, columnId, tagName, startDate, endDate, null);
    }

    @Transactional
    public Task updateTask(Long taskId, String title, String description, Integer position, 
                         Long columnId, String tagName, String startDate, String endDate, Long boardId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));

        if (title != null) {
            task.setTitle(title);
        }

        if (description != null) {
            task.setDescription(description);
        }

        if (position != null) {
            task.setPosition(position);
        }

        if (columnId != null && !columnId.equals(task.getColumn().getId())) {
            DashBoardColumn column = columnRepository.findById(columnId)
                    .orElseThrow(() -> new IllegalArgumentException("Column not found with id: " + columnId));
            task.setColumn(column);
        }

        if (tagName != null) {
            if (tagName.isEmpty()) {
                task.setTag(null);
                logger.info("Cleared tag for task: {}", taskId);
            } else {
                boolean tagFound = false;
                
                // Try first with explicit boardId
                if (boardId != null) {
                    logger.info("Searching for tag by name: {} and explicit boardId: {}", tagName, boardId);
                    Optional<Tag> tagByExplicitBoardId = tagRepository.findByNameAndBoardId(tagName, boardId);
                    if (tagByExplicitBoardId.isPresent()) {
                        logger.info("Found tag by explicit boardId: {}", tagByExplicitBoardId.get().getId());
                        task.setTag(tagByExplicitBoardId.get());
                        tagFound = true;
                    }
                }
                
                // If not found yet, try with column's board
                if (!tagFound) {
                    // Get boardId from current column
                    Long columnBoardId = task.getColumn().getBoard().getId();
                    logger.info("Searching for tag by name: {} and column's boardId: {}", tagName, columnBoardId);
                    
                    // Try to find by name and board ID
                    Optional<Tag> tagByColumnBoardId = tagRepository.findByNameAndBoardId(tagName, columnBoardId);
                    
                    if (tagByColumnBoardId.isPresent()) {
                        logger.info("Found tag by column's boardId: {}", tagByColumnBoardId.get().getId());
                        task.setTag(tagByColumnBoardId.get());
                        tagFound = true;
                    } else {
                        // Fallback to global search
                        Optional<Tag> tagByName = tagRepository.findByName(tagName);
                        if (tagByName.isPresent()) {
                            logger.info("Found tag by name only: {}", tagByName.get().getId());
                            task.setTag(tagByName.get());
                            tagFound = true;
                        }
                    }
                }
                
                // If tag still not found, create a new one
                if (!tagFound) {
                    logger.info("Creating new tag with name: {}", tagName);
                    Board board = task.getColumn().getBoard();
                    Tag newTag = new Tag(tagName, generateRandomColor(), board);
                    board.addTag(newTag);
                    tagRepository.save(newTag);
                    task.setTag(newTag);
                    logger.info("Created new tag with ID: {}", newTag.getId());
                }
            }
        }
        
        if (startDate != null) {
            if (startDate.isEmpty()) {
                task.setStartDate(null);
            } else {
                task.setStartDate(LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME));
            }
        }
        
        if (endDate != null) {
            if (endDate.isEmpty()) {
                task.setEndDate(null);
            } else {
                task.setEndDate(LocalDateTime.parse(endDate, DateTimeFormatter.ISO_DATE_TIME));
            }
        }
        
        return saveAndLogTask(task);
    }
    
    @Transactional
    public Task updateTaskWithTagId(Long taskId, String title, String description, Integer position, 
                                  Long columnId, Long tagId, String startDate, String endDate) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));

        if (title != null) {
            task.setTitle(title);
        }

        if (description != null) {
            task.setDescription(description);
        }

        if (position != null) {
            task.setPosition(position);
        }

        if (columnId != null && !columnId.equals(task.getColumn().getId())) {
            DashBoardColumn column = columnRepository.findById(columnId)
                    .orElseThrow(() -> new IllegalArgumentException("Column not found with id: " + columnId));
            task.setColumn(column);
        }

        if (tagId != null) {
            if (tagId == -1) { // Special value to indicate tag removal
                task.setTag(null);
                logger.info("Cleared tag for task: {}", taskId);
            } else {
                tagRepository.findById(tagId).ifPresent(tag -> {
                    logger.info("Found tag with id: {}", tagId);
                    task.setTag(tag);
                });
            }
        }
        
        if (startDate != null) {
            if (startDate.isEmpty()) {
                task.setStartDate(null);
            } else {
                task.setStartDate(LocalDateTime.parse(startDate, DateTimeFormatter.ISO_DATE_TIME));
            }
        }
        
        if (endDate != null) {
            if (endDate.isEmpty()) {
                task.setEndDate(null);
            } else {
                task.setEndDate(LocalDateTime.parse(endDate, DateTimeFormatter.ISO_DATE_TIME));
            }
        }
        
        return saveAndLogTask(task);
    }

    @Transactional
    public Task saveAndLogTask(Task task) {
        Task savedTask = taskRepository.save(task);
        logger.info("Saved task with ID: {}", savedTask.getId());
        
        // Force refresh the task from the database to ensure all collections are properly loaded
        taskRepository.flush();
        Task refreshedTask = taskRepository.findById(savedTask.getId())
                .orElseThrow(() -> new RuntimeException("Task not found after saving: " + savedTask.getId()));
        
        logger.info("Task {} has {} participants after saving", refreshedTask.getId(), refreshedTask.getParticipants().size());
        
        return refreshedTask;
    }

    @Transactional
    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public void deleteAllTasksByColumn(Long columnId) {
        taskRepository.deleteByColumnId(columnId);
    }

    @Transactional
    public void updateTasksPositions(List<Long> taskIds) {
        for (int i = 0; i < taskIds.size(); i++) {
            final int position = i;
            Long taskId = taskIds.get(i);
            taskRepository.findById(taskId).ifPresent(task -> {
                task.setPosition(position);
                taskRepository.save(task);
            });
        }
    }

    @Transactional
    public Task moveTaskToColumn(Long taskId, Long columnId, Integer position) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));
        
        DashBoardColumn sourceColumn = task.getColumn();
        DashBoardColumn targetColumn = columnRepository.findById(columnId)
                .orElseThrow(() -> new IllegalArgumentException("Column not found with id: " + columnId));
        
        // If moving within the same column
        if (sourceColumn.getId().equals(targetColumn.getId())) {
            // Get all tasks in the column ordered by position
            List<Task> tasks = taskRepository.findByColumnOrderByPosition(targetColumn);
            
            // Remove the task from its current position
            tasks.remove(task);
            
            // Insert the task at the new position
            tasks.add(position, task);
            
            // Update positions for all tasks
            for (int i = 0; i < tasks.size(); i++) {
                tasks.get(i).setPosition(i);
                taskRepository.save(tasks.get(i));
            }
        } else {
            // Moving to a different column
            // Get tasks from both columns
            List<Task> sourceTasks = taskRepository.findByColumnOrderByPosition(sourceColumn);
            List<Task> targetTasks = taskRepository.findByColumnOrderByPosition(targetColumn);
            
            // Remove task from source column
            sourceTasks.remove(task);
            
            // Update positions in source column
            for (int i = 0; i < sourceTasks.size(); i++) {
                sourceTasks.get(i).setPosition(i);
                taskRepository.save(sourceTasks.get(i));
            }
            
            // Insert task into target column
            targetTasks.add(position, task);
            task.setColumn(targetColumn);
            
            // Update positions in target column
            for (int i = 0; i < targetTasks.size(); i++) {
                targetTasks.get(i).setPosition(i);
                taskRepository.save(targetTasks.get(i));
            }
        }
        
        return task;
    }

    @Transactional
    public Task addParticipantToTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        task.addParticipant(user);
        return taskRepository.save(task);
    }

    @Transactional
    public Task removeParticipantFromTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        task.removeParticipant(user);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Set<User> getTaskParticipants(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with id: " + taskId));
        
        return task.getParticipants();
    }
    
    /**
     * Получает все задачи, в которых пользователь является участником
     * @param userId ID пользователя
     * @return список задач пользователя
     */
    @Transactional(readOnly = true)
    public List<Task> getAllUserTasks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        return taskRepository.findByParticipantsContains(user);
    }
    
    /**
     * Получает все задачи пользователя по его ID (альтернативная реализация через запрос)
     * @param userId ID пользователя
     * @return список задач пользователя
     */
    @Transactional(readOnly = true)
    public List<Task> getUserTasksById(Long userId) {
        return taskRepository.findAllTasksByUserId(userId);
    }
    
    private String generateRandomColor() {
        // Generate a random hex color
        String[] colors = {
            "#FF5733", "#33FF57", "#3357FF", "#FF33A8", 
            "#33FFF5", "#F5FF33", "#FF5733", "#C233FF",
            "#FF8C33", "#33FFEC", "#EC33FF", "#FFEC33"
        };
        
        int randomIndex = (int) (Math.random() * colors.length);
        return colors[randomIndex];
    }

    /**
     * Get a column by ID
     * 
     * @param columnId Column ID
     * @return The column, or null if not found
     */
    @Transactional(readOnly = true)
    public DashBoardColumn getColumnById(Long columnId) {
        return columnRepository.findById(columnId).orElse(null);
    }
} 