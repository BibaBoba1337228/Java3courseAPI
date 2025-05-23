package course.project.API.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");  // Префикс для входящих сообщений
        config.setUserDestinationPrefix("/user");  // Префикс для адресации сообщений пользователям
        logger.info("WebSocket broker configured: outbound prefixes=/topic,/queue, inbound prefix=/app, user prefix=/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        logger.info("Registering STOMP endpoints...");
        
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        logger.info("Registered endpoint: /ws with SockJS support");
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        logger.info("Configuring client inbound channel");
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    logger.info("WebSocket message type: {}, command: {}", message.getClass().getSimpleName(), accessor.getCommand());
                    
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        logger.info("Processing CONNECT command in WebSocket interceptor");
                        
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        
                        if (authentication != null && authentication.isAuthenticated()) {
                            logger.info("Found authenticated user: {}, principal type: {}", 
                                     authentication.getName(), 
                                     authentication.getPrincipal().getClass().getSimpleName());
                            accessor.setUser(authentication);
                        } else {
                            logger.warn("No authenticated user found for WebSocket connection");
                        }
                    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        logger.info("User subscribing to destination: {}", accessor.getDestination());
                        if (accessor.getUser() != null) {
                            logger.info("Subscription from user: {}", accessor.getUser().getName());
                        } else {
                            logger.warn("Subscription without user information");
                        }
                    } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                        logger.info("User sending message to destination: {}", accessor.getDestination());
                        if (accessor.getUser() != null) {
                            logger.info("Message from user: {}", accessor.getUser().getName());
                        } else {
                            logger.warn("Message without user information");
                        }
                    }
                }
                
                return message;
            }
        });
    }
} 