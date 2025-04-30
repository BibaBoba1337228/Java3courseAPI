package course.project.API.controllers;

import course.project.API.models.ChecklistItem;
import course.project.API.services.ChecklistItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checklist-items")
public class ChecklistItemController {

    private final ChecklistItemService checklistItemService;

    @Autowired
    public ChecklistItemController(ChecklistItemService checklistItemService) {
        this.checklistItemService = checklistItemService;
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<ChecklistItem>> getChecklistItemsByTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(checklistItemService.getAllChecklistItemsByTask(taskId));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ChecklistItem> getChecklistItemById(@PathVariable Long itemId) {
        return checklistItemService.getChecklistItemById(itemId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ChecklistItem> createChecklistItem(@RequestBody Map<String, Object> payload) {
        Long taskId = Long.parseLong(payload.get("taskId").toString());
        String text = payload.get("text").toString();
        Integer position = payload.containsKey("position") ? 
                Integer.parseInt(payload.get("position").toString()) : 0;

        ChecklistItem item = checklistItemService.createChecklistItem(taskId, text, position);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ChecklistItem> updateChecklistItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> payload) {
        
        String text = payload.containsKey("text") ? payload.get("text").toString() : null;
        Boolean completed = payload.containsKey("completed") ? 
                Boolean.parseBoolean(payload.get("completed").toString()) : null;
        Integer position = payload.containsKey("position") ? 
                Integer.parseInt(payload.get("position").toString()) : null;

        ChecklistItem item = checklistItemService.updateChecklistItem(itemId, text, completed, position);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteChecklistItem(@PathVariable Long itemId) {
        checklistItemService.deleteChecklistItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Void> deleteAllChecklistItemsByTask(@PathVariable Long taskId) {
        checklistItemService.deleteAllChecklistItemsByTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderChecklistItems(@RequestBody List<Long> itemIds) {
        checklistItemService.updateChecklistItemsPositions(itemIds);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{itemId}/toggle")
    public ResponseEntity<Void> toggleChecklistItemCompleted(@PathVariable Long itemId) {
        checklistItemService.toggleChecklistItemCompleted(itemId);
        return ResponseEntity.ok().build();
    }
} 