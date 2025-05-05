package course.project.API.controllers;

import course.project.API.dto.board.ColumnWithTasksDTO;
import course.project.API.dto.board.TaskDTO;
import course.project.API.dto.websocket.WebSocketMessage;
import course.project.API.models.DashBoardColumn;
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

    @Autowired
    public BoardWebSocketController(WebSocketService webSocketService, BoardService boardService) {
        this.webSocketService = webSocketService;
        this.boardService = boardService;
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
        webSocketService.sendMessageToBoard(boardId, "COLUMN_UPDATED", payload);
    }
    
    private void handleDeleteColumn(Long boardId, Map<String, Object> payload) {
        logger.info("Processing DELETE_COLUMN for board {}", boardId);
        webSocketService.sendMessageToBoard(boardId, "COLUMN_DELETED", payload);
    }
    
    private void handleReorderColumns(Long boardId, Map<String, Object> payload) {
        logger.info("Processing REORDER_COLUMNS for board {}", boardId);
        webSocketService.sendMessageToBoard(boardId, "COLUMNS_REORDERED", payload);
    }
    
    private void handleCreateTask(Long boardId, Map<String, Object> payload) {
        logger.info("Processing CREATE_TASK for board {}", boardId);
        webSocketService.sendMessageToBoard(boardId, "TASK_CREATED", payload);
    }
    
    private void handleUpdateTask(Long boardId, Map<String, Object> payload) {
        logger.info("Processing UPDATE_TASK for board {}", boardId);
        webSocketService.sendMessageToBoard(boardId, "TASK_UPDATED", payload);
    }
    
    private void handleDeleteTask(Long boardId, Map<String, Object> payload) {
        logger.info("Processing DELETE_TASK for board {}", boardId);
        webSocketService.sendMessageToBoard(boardId, "TASK_DELETED", payload);
    }
    
    private void handleMoveTask(Long boardId, Map<String, Object> payload) {
        logger.info("Processing MOVE_TASK for board {}", boardId);
        webSocketService.sendMessageToBoard(boardId, "TASK_MOVED", payload);
    }
} 