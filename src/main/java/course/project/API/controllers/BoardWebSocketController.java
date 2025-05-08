package course.project.API.controllers;

import course.project.API.services.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class BoardWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(BoardWebSocketController.class);

    private final WebSocketService webSocketService;

    @Autowired
    public BoardWebSocketController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @MessageMapping("/boards/{boardId}/connect")
    public void connectToBoard(@DestinationVariable Long boardId, 
                               SimpMessageHeaderAccessor headerAccessor,
                               Principal principal) {
        logger.info("Message received at /boards/{}/connect", boardId);
        
        if (principal == null) {
            logger.error("Principal is null - unauthenticated user attempted to connect to board {}", boardId);
            return;
        }
        
        String username = principal.getName();
        logger.info("User {} attempting to connect to board {}", username, boardId);
        
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("boardId", boardId);
        logger.info("Added session attributes: username={}, boardId={}", username, boardId);
        
        try {
            webSocketService.notifyUserJoinedBoard(boardId, username);
            logger.info("Successfully notified other users about {} joining board {}", username, boardId);
        } catch (Exception e) {
            logger.error("Error notifying about user join: {}", e.getMessage(), e);
        }
    }

} 