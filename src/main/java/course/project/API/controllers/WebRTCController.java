package course.project.API.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Controller for WebRTC configuration
 */
@RestController
@RequestMapping("/api/webrtc")
public class WebRTCController {

    /**
     * Returns WebRTC configuration including ICE servers (STUN/TURN)
     * @return WebRTC configuration
     */
    @GetMapping("/config")
    public Map<String, Object> getWebRTCConfig() {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, Object>> iceServers = new ArrayList<>();
        
        // Add Google STUN server
        Map<String, Object> stunServer = new HashMap<>();
        stunServer.put("urls", "stun:stun.l.google.com:19302");
        iceServers.add(stunServer);
        
        // Add TURN server with credentials
        Map<String, Object> turnServer = new HashMap<>();
        turnServer.put("urls", "turn:your-turn-server-domain.com:3478");
        turnServer.put("username", "turnuser");  // Replace with your actual username
        turnServer.put("credential", "turnpassword");  // Replace with your actual password
        iceServers.add(turnServer);
        
        config.put("iceServers", iceServers);
        
        // Additional WebRTC configuration if needed
        config.put("iceCandidatePoolSize", 10);
        config.put("bundlePolicy", "max-bundle");
        
        return config;
    }
} 