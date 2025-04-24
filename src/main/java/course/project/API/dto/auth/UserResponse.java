package course.project.API.dto.auth;

public class UserResponse {
    private String username;
    private String name;
    private String avatarURL;

    public UserResponse(String username, String name, String avatarURL) {
        this.username = username;
        this.name = name;
        this.avatarURL = avatarURL;
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
} 