package course.project.API.dto.chatSocket;

import course.project.API.models.ChatRole;

public class UserRoleChangedPayload {
    private Long userId;
    private ChatRole newRole;

    public UserRoleChangedPayload() {
    }

    public UserRoleChangedPayload(Long userId, ChatRole newRole) {
        this.userId = userId;
        this.newRole = newRole;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ChatRole getNewRole() {
        return newRole;
    }

    public void setNewRole(ChatRole newRole) {
        this.newRole = newRole;
    }
}
