package course.project.API.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    // Короче я уже третий раз это переписываю, если использовать крутой модный молодежный EnableWebSocketSecurity,
    // то нужно включать csrf, а это супер впадлу, так что у нас вот такой вот старый добрый deprecated код будет
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .simpTypeMatchers(SimpMessageType.DISCONNECT).permitAll()
            .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.HEARTBEAT, SimpMessageType.UNSUBSCRIBE).authenticated()
            .simpDestMatchers(
                    "/app/boards/**",
                    "/app/chat/**",
                    "/app/call/**",
                    "/user/queue/private/**",
                    "/topic/boards/**",
                    "/topic/chat/**"
            )
                .authenticated()
            .anyMessage().denyAll();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}