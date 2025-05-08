package course.project.API.dto.user;

import course.project.API.models.InvitationStatus;

public class UserResponse {
    private Long id;
    private String name;
    private String avatarURL;
    private InvitationStatus status;
    
    public UserResponse() {
    }

    public UserResponse(Long id, String name, String avatarURL) {
        this.id = id;
        this.name = name;
        this.avatarURL = avatarURL;
    }

    public UserResponse(Long id, String name, String avatarURL, InvitationStatus status) {
        this.id = id;
        this.name = name;
        this.avatarURL = avatarURL;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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