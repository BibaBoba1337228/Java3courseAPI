package course.project.API.dto.user;

public class UserResponse {
    private Long id;
    private String name;
    private String avatarURL;

    public UserResponse() {
    }

    public UserResponse(Long id, String name, String avatarURL) {
        this.id = id;
        this.name = name;
        this.avatarURL = avatarURL;
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
    
}