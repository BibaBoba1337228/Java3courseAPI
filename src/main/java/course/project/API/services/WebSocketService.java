package course.project.API.services;

import course.project.API.dto.websocket.WebSocketMessage;
import course.project.API.dto.websocket.WebSocketMessageViaObject;
import course.project.API.models.User;
import course.project.API.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    
    // Храним информацию о подключенных пользователях по boardId
    private final Map<Long, Map<String, Object>> boardSessions = new ConcurrentHashMap<>();

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    /**
     * Отправляет сообщение всем клиентам, подключенным к конкретной доске
     */
    public void sendMessageToBoard(Long boardId, String type, Object payload) {
        WebSocketMessageViaObject message = new WebSocketMessageViaObject(type, payload);
        messagingTemplate.convertAndSend("/topic/boards/" + boardId, message);
    }

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

    public void sendPrivateMessageToUser(String username, Object event) {
        messagingTemplate.convertAndSendToUser(username, "/queue/private", event);
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

    /**
     * Notifies all project participants about an event related to a project
     */
    public void notifyProjectParticipants(Long projectId, String type, Object payload) {
        WebSocketMessage message = new WebSocketMessage(type, convertObjectToMap(payload));
        messagingTemplate.convertAndSend("/topic/projects/" + projectId, message);
    }

    /**
     * Converts an object to a Map for WebSocket payload
     */
    private Map<String, Object> convertObjectToMap(Object object) {
        if (object instanceof Map) {
            return (Map<String, Object>) object;
        }
        
        // For DTOs and other objects, create a simple map with the object as the "data" field
        Map<String, Object> map = new HashMap<>();
        map.put("data", object);
        return map;
    }

    /**
     * Получает пользователя по ID
     * @param userId ID пользователя
     * @return Объект User или null, если пользователь не найден
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
} 