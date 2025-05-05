package course.project.API.services;

import course.project.API.dto.websocket.WebSocketMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    
    // Храним информацию о подключенных пользователях по boardId
    private final Map<Long, Map<String, Object>> boardSessions = new ConcurrentHashMap<>();

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Отправляет сообщение всем клиентам, подключенным к конкретной доске
     */
    public void sendMessageToBoard(Long boardId, String type, Map<String, Object> payload) {
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSend("/topic/boards/" + boardId, message);
    }

    /**
     * Отправляет личное сообщение конкретному пользователю
     */
    public void sendPrivateMessageToUser(String username, String type, Map<String, Object> payload) {
        WebSocketMessage message = new WebSocketMessage(type, payload);
        messagingTemplate.convertAndSendToUser(username, "/queue/private", message);
    }

    /**
     * Отправляет уведомление о подключении пользователя к доске
     */
    public void notifyUserJoinedBoard(Long boardId, String username) {
        // Добавляем пользователя в список подключенных к доске
        boardSessions.computeIfAbsent(boardId, k -> new ConcurrentHashMap<>())
                     .put(username, new HashMap<>());
        
        // Создаем payload с информацией о подключении
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("action", "joined");
        
        // Отправляем сообщение о подключении пользователя
        sendMessageToBoard(boardId, "USER_CONNECTION", payload);
    }

    /**
     * Удаляет информацию о сессии пользователя при отключении
     */
    public void handleUserDisconnect(Long boardId, String username) {
        if (boardSessions.containsKey(boardId)) {
            boardSessions.get(boardId).remove(username);
            
            // Если больше нет подключенных пользователей, удаляем информацию о доске
            if (boardSessions.get(boardId).isEmpty()) {
                boardSessions.remove(boardId);
            }
            
            // Создаем payload с информацией об отключении
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("action", "left");
            
            // Отправляем сообщение об отключении пользователя
            sendMessageToBoard(boardId, "USER_CONNECTION", payload);
        }
    }

    /**
     * Получает количество пользователей, подключенных к доске
     */
    public int getBoardConnectionCount(Long boardId) {
        return boardSessions.containsKey(boardId) ? boardSessions.get(boardId).size() : 0;
    }
} 