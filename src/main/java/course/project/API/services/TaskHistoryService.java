package course.project.API.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import course.project.API.models.Task;
import course.project.API.models.TaskHistory;
import course.project.API.models.User;
import course.project.API.repositories.TaskHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskHistoryService.class);
    
    private final TaskHistoryRepository taskHistoryRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TaskHistoryService(TaskHistoryRepository taskHistoryRepository) {
        this.taskHistoryRepository = taskHistoryRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTaskCreation(User user, Task task) {
        try {
            String taskJson = objectMapper.writeValueAsString(task);
            Long boardId = task.getColumn().getBoard().getId();
            
            TaskHistory history = new TaskHistory(
                "CREATE",
                user,
                task.getId(),
                boardId,
                null, // No old task for creation
                taskJson,
                taskJson // Use same JSON for snapshot
            );
            
            taskHistoryRepository.save(history);
            logger.info("Recorded task creation: TaskID={}, UserID={}", task.getId(), user.getId());
        } catch (Exception e) {
            logger.error("Failed to record task creation: {}", e.getMessage(), e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTaskUpdate(User user, Task oldTask, Task newTask) {
        try {
            String oldTaskJson = objectMapper.writeValueAsString(oldTask);
            String newTaskJson = objectMapper.writeValueAsString(newTask);
            Long boardId = newTask.getColumn().getBoard().getId();
            
            TaskHistory history = new TaskHistory(
                "UPDATE",
                user,
                newTask.getId(),
                boardId,
                oldTaskJson,
                newTaskJson,
                newTaskJson // Use new task JSON for snapshot
            );
            
            taskHistoryRepository.save(history);
            logger.info("Recorded task update: TaskID={}, UserID={}", newTask.getId(), user.getId());
        } catch (Exception e) {
            logger.error("Failed to record task update: {}", e.getMessage(), e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTaskDeletion(User user, Task task) {
        try {
            String taskJson = objectMapper.writeValueAsString(task);
            Long boardId = task.getColumn().getBoard().getId();
            
            TaskHistory history = new TaskHistory(
                "DELETE",
                user,
                task.getId(),
                boardId,
                taskJson,
                null, // No new task for deletion
                taskJson // Use deleted task JSON for snapshot
            );
            
            taskHistoryRepository.save(history);
            logger.info("Recorded task deletion: TaskID={}, UserID={}", task.getId(), user.getId());
        } catch (Exception e) {
            logger.error("Failed to record task deletion: {}", e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<TaskHistory> getTaskHistoryForTask(Long taskId) {
        return taskHistoryRepository.findByTaskIdOrderByTimestampDesc(taskId);
    }
    
    @Transactional(readOnly = true)
    public List<TaskHistory> getTaskHistoryForBoard(Long boardId) {
        return taskHistoryRepository.findByBoardIdOrderByTimestampDesc(boardId);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteTaskHistory(Long taskId) {
        try {
            taskHistoryRepository.deleteByTaskId(taskId);
            logger.info("Deleted task history for TaskID={}", taskId);
        } catch (Exception e) {
            logger.error("Failed to delete task history: {}", e.getMessage(), e);
            throw e; // Rethrow to let calling code handle the error
        }
    }
} 