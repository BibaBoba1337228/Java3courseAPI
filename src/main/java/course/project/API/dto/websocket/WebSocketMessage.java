package course.project.API.dto.websocket;

import java.util.HashMap;
import java.util.Map;

public class WebSocketMessage {
    private String type;
    private Map<String, Object> payload;

    public WebSocketMessage() {
        this.payload = new HashMap<>();
    }

    public WebSocketMessage(String type) {
        this.type = type;
        this.payload = new HashMap<>();
    }

    public WebSocketMessage(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public void addToPayload(String key, Object value) {
        this.payload.put(key, value);
    }
} 