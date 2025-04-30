package course.project.API.services;

import course.project.API.models.ChecklistItem;
import course.project.API.models.Task;
import course.project.API.repositories.ChecklistItemRepository;
import course.project.API.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class ChecklistItemService {

    private static final Logger logger = LoggerFactory.getLogger(ChecklistItemService.class);

    private final ChecklistItemRepository checklistItemRepository;
    private final TaskRepository taskRepository;

    @Autowired
    public ChecklistItemService(ChecklistItemRepository checklistItemRepository, TaskRepository taskRepository) {
        this.checklistItemRepository = checklistItemRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<ChecklistItem> getAllChecklistItemsByTask(Long taskId) {
        return checklistItemRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItem> getChecklistItemsByTaskOrdered(Task task) {
        return checklistItemRepository.findByTaskOrderByPosition(task);
    }

    @Transactional(readOnly = true)
    public Optional<ChecklistItem> getChecklistItemById(Long itemId) {
        return checklistItemRepository.findById(itemId);
    }

    @Transactional
    public ChecklistItem createChecklistItem(Long taskId, String text, Integer position) {
        logger.info("Creating checklist item for task: {}, text: {}, position: {}", taskId, text, position);
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        
        ChecklistItem item = new ChecklistItem(text, task);
        item.setPosition(position);
        
        ChecklistItem savedItem = checklistItemRepository.save(item);
        logger.info("Saved checklist item with ID: {}", savedItem.getId());
        
        // Add to task's checklist collection for proper retrieval
        task.addChecklistItem(savedItem);
        taskRepository.save(task);
        
        return savedItem;
    }

    @Transactional
    public ChecklistItem updateChecklistItem(Long itemId, String text, Boolean completed, Integer position) {
        logger.info("Updating checklist item: {}, text: {}, completed: {}, position: {}", 
            itemId, text, completed, position);
            
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found with id: " + itemId));
        
        if (text != null) {
            item.setText(text);
        }
        
        if (completed != null) {
            item.setCompleted(completed);
        }
        
        if (position != null) {
            item.setPosition(position);
        }
        
        ChecklistItem updatedItem = checklistItemRepository.save(item);
        logger.info("Updated checklist item: {}", updatedItem.getId());
        
        return updatedItem;
    }

    @Transactional
    public void deleteChecklistItem(Long itemId) {
        checklistItemRepository.deleteById(itemId);
    }

    @Transactional
    public void deleteAllChecklistItemsByTask(Long taskId) {
        logger.info("Deleting all checklist items for task: {}", taskId);
        checklistItemRepository.deleteByTaskId(taskId);
    }

    @Transactional
    public void updateChecklistItemsPositions(List<Long> itemIds) {
        for (int i = 0; i < itemIds.size(); i++) {
            Long itemId = itemIds.get(i);
            ChecklistItem item = checklistItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Checklist item not found with id: " + itemId));
            item.setPosition(i);
            checklistItemRepository.save(item);
        }
    }

    @Transactional
    public void toggleChecklistItemCompleted(Long itemId) {
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Checklist item not found with id: " + itemId));
        
        item.setCompleted(!item.isCompleted());
        checklistItemRepository.save(item);
    }
} 