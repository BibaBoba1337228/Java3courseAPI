package course.project.API.controllers;

import course.project.API.dto.board.ColumnWithTasksDTO;
import course.project.API.dto.board.TaskDTO;
import course.project.API.dto.websocket.WebSocketMessage;
import course.project.API.models.BoardRight;
import course.project.API.models.DashBoardColumn;
import course.project.API.services.BoardRightService;
import course.project.API.services.BoardService;
import course.project.API.services.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BoardWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(BoardWebSocketController.class);

    private final WebSocketService webSocketService;
    private final BoardService boardService;
    private final BoardRightService boardRightService;

    @Autowired
    public BoardWebSocketController(WebSocketService webSocketService, BoardService boardService, BoardRightService boardRightService) {
        this.webSocketService = webSocketService;
        this.boardService = boardService;
        this.boardRightService = boardRightService;
    }

    // Обработка подключения к доске
    @MessageMapping("/boards/{boardId}/connect")
    public void connectToBoard(@DestinationVariable Long boardId, 
                               SimpMessageHeaderAccessor headerAccessor,
                               Principal principal) {
        logger.info("Message received at /boards/{}/connect", boardId);
        
        if (principal == null) {
            logger.error("Principal is null - unauthenticated user attempted to connect to board {}", boardId);
            return;
        }
        
        // Добавляем пользователя в список подключенных к доске
        String username = principal.getName();
        logger.info("User {} attempting to connect to board {}", username, boardId);
        
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("boardId", boardId);
        logger.info("Added session attributes: username={}, boardId={}", username, boardId);
        
        // Можно отправить уведомление остальным пользователям о подключении
        try {
            webSocketService.notifyUserJoinedBoard(boardId, username);
            logger.info("Successfully notified other users about {} joining board {}", username, boardId);
        } catch (Exception e) {
            logger.error("Error notifying about user join: {}", e.getMessage(), e);
        }
    }

    // Обработка различных действий с доской
    @MessageMapping("/boards/{boardId}")
    public void handleBoardAction(@DestinationVariable Long boardId,
                                  @Payload WebSocketMessage message,
                                  Principal principal) {
        if (boardId == null) {
            logger.error("Board ID is required");
            return;
        }

        String username = principal.getName();
        String actionType = message.getType();
        Map<String, Object> payload = message.getPayload();
        
        logger.info("Board action: type={}, boardId={}, user={}", actionType, boardId, username);
        
        // Проверка прав пользователя на доступ к доске
        if (!boardService.hasUserAccessToBoard(username, boardId)) {
            logger.error("User {} does not have access to board {}", username, boardId);

            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("error", "Access denied");
            errorPayload.put("action", actionType);
            webSocketService.sendPrivateMessageToUser(username, "ERROR", errorPayload);
            return;
        }
        
        // Добавляем информацию о пользователе, инициировавшем действие
        payload.put("initiatedBy", username);
        
        switch (actionType) {
            case "CREATE_COLUMN":
                handleCreateColumn(boardId, payload);
                break;
            case "UPDATE_COLUMN":
                handleUpdateColumn(boardId, payload);
                break;
            case "DELETE_COLUMN":
                handleDeleteColumn(boardId, payload);
                break;
            case "REORDER_COLUMNS":
                handleReorderColumns(boardId, payload);
                break;
            case "CREATE_TASK":
                handleCreateTask(boardId, payload);
                break;
            case "UPDATE_TASK":
                handleUpdateTask(boardId, payload);
                break;
            case "DELETE_TASK":
                handleDeleteTask(boardId, payload);
                break;
            case "MOVE_TASK":
                handleMoveTask(boardId, payload);
                break;
            default:
                logger.warn("Unknown action type: {}", actionType);
                break;
        }
    }
    
    // Обработчики конкретных действий
    
    private void handleCreateColumn(Long boardId, Map<String, Object> payload) {
        logger.info("Processing CREATE_COLUMN for board {}", boardId);
        
        try {
            // Извлекаем данные из payload
            String title = (String) payload.get("title");
            Integer position = (Integer) payload.get("position");
            
            if (title == null) {
                logger.error("Title is required for column creation");
                return;
            }
            
            // Создаем колонку через сервис
            DashBoardColumn column = boardService.createColumn(boardId, title, position);
            
            if (column != null) {
                // Добавляем ID созданной колонки в ответ
                payload.put("columnId", column.getId());
                // Уведомляем всех о создании колонки
                webSocketService.sendMessageToBoard(boardId, "COLUMN_CREATED", payload);
                logger.info("Column created successfully: {}", column.getId());
            } else {
                logger.error("Failed to create column for board {}", boardId);
            }
        } catch (Exception e) {
            logger.error("Error creating column: {}", e.getMessage(), e);
        }
    }
    
    private void handleUpdateColumn(Long boardId, Map<String, Object> payload) {
        logger.info("Processing UPDATE_COLUMN for board {}", boardId);
        
        try {
            Object columnIdObj = payload.get("columnId");
            if (columnIdObj == null) {
                logger.error("Column ID is required for update");
                return;
            }
            
            Long columnId = Long.valueOf(columnIdObj.toString());
            String title = (String) payload.get("title");
            Integer position = (Integer) payload.get("position");
            
            if (title == null) {
                logger.error("Title is required for column update");
                return;
            }
            
            // Обновляем колонку через сервис
            DashBoardColumn column = boardService.updateColumn(boardId, columnId, title, position);
            
            if (column != null) {
                // Уведомляем всех об обновлении колонки
                webSocketService.sendMessageToBoard(boardId, "COLUMN_UPDATED", payload);
                logger.info("Column updated successfully: {}", columnId);
            } else {
                logger.error("Failed to update column {} for board {}", columnId, boardId);
            }
        } catch (Exception e) {
            logger.error("Error updating column: {}", e.getMessage(), e);
        }
    }
    
    private void handleDeleteColumn(Long boardId, Map<String, Object> payload) {
        logger.info("Processing DELETE_COLUMN for board {}", boardId);
        
        try {
            Object columnIdObj = payload.get("columnId");
            if (columnIdObj == null) {
                logger.error("Column ID is required for deletion");
                return;
            }
            
            Long columnId = Long.valueOf(columnIdObj.toString());
            
            // Удаляем колонку через сервис
            boolean deleted = boardService.deleteColumn(boardId, columnId);
            
            if (deleted) {
                // Уведомляем всех об удалении колонки
                webSocketService.sendMessageToBoard(boardId, "COLUMN_DELETED", payload);
                logger.info("Column deleted successfully: {}", columnId);
            } else {
                logger.error("Failed to delete column {} for board {}", columnId, boardId);
            }
        } catch (Exception e) {
            logger.error("Error deleting column: {}", e.getMessage(), e);
        }
    }
    
    private void handleReorderColumns(Long boardId, Map<String, Object> payload) {
        logger.info("Processing REORDER_COLUMNS for board {}", boardId);
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columns = (List<Map<String, Object>>) payload.get("columns");
            
            if (columns == null || columns.isEmpty()) {
                logger.error("Columns list is required for reordering");
                return;
            }
            
            // Проверяем наличие необходимых полей в каждой колонке
            for (Map<String, Object> column : columns) {
                if (!column.containsKey("id") || !column.containsKey("position")) {
                    logger.error("Each column must have both 'id' and 'position' fields");
                    return;
                }
                
                // Проверяем, что position не null
                if (column.get("position") == null) {
                    logger.error("Column position cannot be null");
                    return;
                }
            }
            
            // Обновляем порядок колонок через сервис
            boolean reordered = boardService.reorderColumns(boardId, columns);
            
            if (reordered) {
                // Уведомляем всех о новом порядке колонок
                webSocketService.sendMessageToBoard(boardId, "COLUMNS_REORDERED", payload);
                logger.info("Columns reordered successfully for board {}", boardId);
            } else {
                logger.error("Failed to reorder columns for board {}", boardId);
            }
        } catch (Exception e) {
            logger.error("Error reordering columns: {}", e.getMessage(), e);
        }
    }
    
    private void handleCreateTask(Long boardId, Map<String, Object> payload) {
        logger.info("Processing CREATE_TASK for board {}", boardId);
        
        try {
            // Проверяем права пользователя на создание задач
            String username = (String) payload.get("initiatedBy");
            if (username == null) {
                logger.error("Username is required for task creation");
                return;
            }
            
            // Проверка прав на создание задач
            if (!boardRightService.hasBoardRightByUsername(boardId, username, BoardRight.CREATE_TASKS)) {
                logger.error("User {} does not have CREATE_TASKS right for board {}", username, boardId);
                
                Map<String, Object> errorPayload = new HashMap<>();
                errorPayload.put("error", "Access denied");
                errorPayload.put("action", "CREATE_TASK");
                webSocketService.sendPrivateMessageToUser(username, "ERROR", errorPayload);
                return;
            }
            
            Object columnIdObj = payload.get("columnId");
            if (columnIdObj == null) {
                logger.error("Column ID is required for task creation");
                return;
            }
            
            Long columnId = Long.valueOf(columnIdObj.toString());
            String title = (String) payload.get("title");
            String description = (String) payload.get("description");
            @SuppressWarnings("unchecked")
            List<Long> tagIds = (List<Long>) payload.get("tagIds");
            
            if (title == null) {
                logger.error("Title is required for task creation");
                return;
            }
            
            // Создаем задачу через сервис
            TaskDTO task = boardService.createTask(boardId, columnId, title, description, tagIds);
            
            if (task != null) {
                // Добавляем ID созданной задачи в ответ
                payload.put("taskId", task.getId());
                // Уведомляем всех о создании задачи
                webSocketService.sendMessageToBoard(boardId, "TASK_CREATED", payload);
                logger.info("Task created successfully: {}", task.getId());
            } else {
                logger.error("Failed to create task for board {}", boardId);
            }
        } catch (Exception e) {
            logger.error("Error creating task: {}", e.getMessage(), e);
        }
    }
    
    private void handleUpdateTask(Long boardId, Map<String, Object> payload) {
        logger.info("Processing UPDATE_TASK for board {}", boardId);
        
        try {
            // Проверяем права пользователя на редактирование задач
            String username = (String) payload.get("initiatedBy");
            if (username == null) {
                logger.error("Username is required for task update");
                return;
            }
            
            // Проверка прав на редактирование задач
            if (!boardRightService.hasBoardRightByUsername(boardId, username, BoardRight.EDIT_TASKS)) {
                logger.error("User {} does not have EDIT_TASKS right for board {}", username, boardId);
                
                Map<String, Object> errorPayload = new HashMap<>();
                errorPayload.put("error", "Access denied");
                errorPayload.put("action", "UPDATE_TASK");
                webSocketService.sendPrivateMessageToUser(username, "ERROR", errorPayload);
                return;
            }
            
            Object taskIdObj = payload.get("taskId");
            if (taskIdObj == null) {
                logger.error("Task ID is required for update");
                return;
            }
            
            Long taskId = Long.valueOf(taskIdObj.toString());
            String title = (String) payload.get("title");
            String description = (String) payload.get("description");
            @SuppressWarnings("unchecked")
            List<Long> tagIds = (List<Long>) payload.get("tagIds");
            
            if (title == null) {
                logger.error("Title is required for task update");
                return;
            }
            
            // Обновляем задачу через сервис
            TaskDTO task = boardService.updateTask(boardId, taskId, title, description, tagIds);
            
            if (task != null) {
                // Уведомляем всех об обновлении задачи
                webSocketService.sendMessageToBoard(boardId, "TASK_UPDATED", payload);
                logger.info("Task updated successfully: {}", taskId);
            } else {
                logger.error("Failed to update task {} for board {}", taskId, boardId);
            }
        } catch (Exception e) {
            logger.error("Error updating task: {}", e.getMessage(), e);
        }
    }
    
    private void handleDeleteTask(Long boardId, Map<String, Object> payload) {
        logger.info("Processing DELETE_TASK for board {}", boardId);
        
        try {
            // Проверяем права пользователя на удаление задач
            String username = (String) payload.get("initiatedBy");
            if (username == null) {
                logger.error("Username is required for task deletion");
                return;
            }
            
            // Проверка прав на удаление задач
            if (!boardRightService.hasBoardRightByUsername(boardId, username, BoardRight.DELETE_TASKS)) {
                logger.error("User {} does not have DELETE_TASKS right for board {}", username, boardId);
                
                Map<String, Object> errorPayload = new HashMap<>();
                errorPayload.put("error", "Access denied");
                errorPayload.put("action", "DELETE_TASK");
                webSocketService.sendPrivateMessageToUser(username, "ERROR", errorPayload);
                return;
            }
            
            Object taskIdObj = payload.get("taskId");
            if (taskIdObj == null) {
                logger.error("Task ID is required for deletion");
                return;
            }
            
            Long taskId = Long.valueOf(taskIdObj.toString());
            
            // Удаляем задачу через сервис
            boolean deleted = boardService.deleteTask(boardId, taskId);
            
            if (deleted) {
                // Уведомляем всех об удалении задачи
                webSocketService.sendMessageToBoard(boardId, "TASK_DELETED", payload);
                logger.info("Task deleted successfully: {}", taskId);
            } else {
                logger.error("Failed to delete task {} for board {}", taskId, boardId);
            }
        } catch (Exception e) {
            logger.error("Error deleting task: {}", e.getMessage(), e);
        }
    }
    
    private void handleMoveTask(Long boardId, Map<String, Object> payload) {
        logger.info("Processing MOVE_TASK for board {}", boardId);
        
        try {
            // Проверяем права пользователя на перемещение задач
            String username = (String) payload.get("initiatedBy");
            if (username == null) {
                logger.error("Username is required for task movement");
                return;
            }
            
            // Проверка прав на перемещение задач
            if (!boardRightService.hasBoardRightByUsername(boardId, username, BoardRight.MOVE_TASKS)) {
                logger.error("User {} does not have MOVE_TASKS right for board {}", username, boardId);
                
                Map<String, Object> errorPayload = new HashMap<>();
                errorPayload.put("error", "Access denied");
                errorPayload.put("action", "MOVE_TASK");
                webSocketService.sendPrivateMessageToUser(username, "ERROR", errorPayload);
                return;
            }
            
            Object taskIdObj = payload.get("taskId");
            Object sourceColumnIdObj = payload.get("sourceColumnId");
            Object targetColumnIdObj = payload.get("targetColumnId");
            
            if (taskIdObj == null || sourceColumnIdObj == null || targetColumnIdObj == null) {
                logger.error("Task ID, source column ID and target column ID are required for moving task");
                return;
            }
            
            Long taskId = Long.valueOf(taskIdObj.toString());
            Long sourceColumnId = Long.valueOf(sourceColumnIdObj.toString());
            Long targetColumnId = Long.valueOf(targetColumnIdObj.toString());
            Integer newPosition = (Integer) payload.get("newPosition");
            
            // Перемещаем задачу через сервис
            boolean moved = boardService.moveTask(boardId, taskId, sourceColumnId, targetColumnId, newPosition);
            
            if (moved) {
                // Уведомляем всех о перемещении задачи
                webSocketService.sendMessageToBoard(boardId, "TASK_MOVED", payload);
                logger.info("Task moved successfully: {} from column {} to {}", taskId, sourceColumnId, targetColumnId);
            } else {
                logger.error("Failed to move task {} for board {}", taskId, boardId);
            }
        } catch (Exception e) {
            logger.error("Error moving task: {}", e.getMessage(), e);
        }
    }
} 