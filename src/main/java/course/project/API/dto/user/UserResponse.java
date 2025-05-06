package course.project.API.dto.user;

import course.project.API.models.InvitationStatus;

public class UserResponse {
    private String username;
    private String name;
    private String avatarURL;
    private InvitationStatus status;
    
    public UserResponse() {
    }

    public UserResponse(String username, String name, String avatarURL) {
        this.username = username;
        this.name = name;
        this.avatarURL = avatarURL;
    }

    public UserResponse(String username, String name, String avatarURL, InvitationStatus status) {
        this.username = username;
        this.name = name;
        this.avatarURL = avatarURL;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }
    
    public InvitationStatus getStatus() {
        return status;
    }
    
    public void setStatus(InvitationStatus status) {
        this.status = status;
    }
} 