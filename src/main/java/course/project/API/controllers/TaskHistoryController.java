package course.project.API.controllers;

import course.project.API.dto.board.TaskHistoryDTO;
import course.project.API.models.BoardRight;
import course.project.API.models.Task;
import course.project.API.models.TaskHistory;
import course.project.API.models.User;
import course.project.API.services.BoardRightService;
import course.project.API.services.TaskHistoryService;
import course.project.API.services.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/task-history")
public class TaskHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(TaskHistoryController.class);

    private final TaskHistoryService taskHistoryService;
    private final TaskService taskService;
    private final BoardRightService boardRightService;

    @Autowired
    public TaskHistoryController(
            TaskHistoryService taskHistoryService,
            TaskService taskService,
            BoardRightService boardRightService) {
        this.taskHistoryService = taskHistoryService;
        this.taskService = taskService;
        this.boardRightService = boardRightService;
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskHistoryDTO>> getTaskHistory(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser) {
        
        Task task = taskService.getTaskById(taskId).orElse(null);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Long boardId = task.getColumn().getBoard().getId();
        
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD)) {
            return ResponseEntity.status(403).build();
        }
        
        List<TaskHistory> history = taskHistoryService.getTaskHistoryForTask(taskId);
        List<TaskHistoryDTO> historyDTOs = history.stream()
                .map(TaskHistoryDTO::new)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(historyDTOs);
    }
    
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<TaskHistoryDTO>> getBoardTaskHistory(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User currentUser) {
        
        if (!boardRightService.hasBoardRight(boardId, currentUser.getId(), BoardRight.VIEW_BOARD)) {
            return ResponseEntity.status(403).build();
        }
        
        List<TaskHistory> history = taskHistoryService.getTaskHistoryForBoard(boardId);
        List<TaskHistoryDTO> historyDTOs = history.stream()
                .map(TaskHistoryDTO::new)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(historyDTOs);
    }
} 