package course.project.API.config;

import course.project.API.services.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final WebSocketService webSocketService;

    @Autowired
    public WebSocketEventListener(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }
    
    @EventListener
    public void handleWebSocketConnectRequest(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        logger.info("WebSocket connection attempt: " + headerAccessor.getSessionId());
        logger.info("Connection headers: " + headerAccessor.toNativeHeaderMap());
        logger.info("Destination: " + headerAccessor.getDestination());
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        logger.info("WebSocket connection established: " + headerAccessor.getSessionId());
        
        // Добавляем детали пользователя, если доступны через Principal
        if (headerAccessor.getUser() != null) {
            logger.info("User connected: " + headerAccessor.getUser().getName());
        }
    }
    
    @EventListener
    public void handleSubscription(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        logger.info("Subscription: session=" + headerAccessor.getSessionId() + 
                    ", destination=" + headerAccessor.getDestination());
    }

    @EventListener
    public void handleUnsubscription(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String subscriptionId = headerAccessor.getFirstNativeHeader("id");
        logger.info("Unsubscription: session=" + headerAccessor.getSessionId() + 
                    ", subscriptionId=" + subscriptionId);
    }


    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        CloseStatus status = event.getCloseStatus();
        
        logger.info("WebSocket disconnect: session=" + headerAccessor.getSessionId() + 
                    ", status=" + (status != null ? status.getCode() + " " + status.getReason() : "unknown"));

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        Long boardId = (Long) headerAccessor.getSessionAttributes().get("boardId");

        if (username != null && boardId != null) {
            logger.info("User Disconnected: " + username + " from board: " + boardId);
            
            // Уведомляем об отключении пользователя
            webSocketService.handleUserDisconnect(boardId, username);
        }
    }
} 