package course.project.API.dto.chatSocket;

public class UserActionPayload {
    private Long userId;
    private String userName;
    private String avatarURL;

    public UserActionPayload() {
    }

    public UserActionPayload(Long userId, String userName, String avatarURL) {
        this.userId = userId;
        this.userName = userName;
        this.avatarURL = avatarURL;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }
}