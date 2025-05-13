package course.project.API.dto.chatSocket;

import course.project.API.models.ChatRole;

public class UserRoleChangedPayload {
    private Long userId;
    private String userName;
    private ChatRole newRole;

    public UserRoleChangedPayload() {
    }

    public UserRoleChangedPayload(Long userId, String userName, ChatRole newRole) {
        this.userId = userId;
        this.userName = userName;
        this.newRole = newRole;
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

    public ChatRole getNewRole() {
        return newRole;
    }

    public void setNewRole(ChatRole newRole) {
        this.newRole = newRole;
    }
}
