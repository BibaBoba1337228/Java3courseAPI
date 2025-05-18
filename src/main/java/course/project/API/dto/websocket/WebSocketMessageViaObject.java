package course.project.API.dto.websocket;

import java.util.Map;

public class WebSocketMessageViaObject {
    private String type;
    private Object payload;

    public WebSocketMessageViaObject() {
    }

    public WebSocketMessageViaObject(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

}