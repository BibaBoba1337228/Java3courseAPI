package course.project.API.dto.chat;

import course.project.API.dto.user.UserResponse;
import course.project.API.models.ChatRole;
import java.util.Map;
import java.util.Set;

public class ChatDTO {
    private Long id;
    private String name;
    private boolean isGroupChat;
    private String avatarURL;
    private Set<UserResponse> participants;
    private Map<Long, ChatRole> userRoles;

    public ChatDTO() {
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


    public boolean isGroupChat() {
        return isGroupChat;
    }

    public void setIsGroupChat(boolean groupChat) {
        isGroupChat = groupChat;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public Set<UserResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<UserResponse> participants) {
        this.participants = participants;
    }

    public Map<Long, ChatRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Map<Long, ChatRole> userRoles) {
        this.userRoles = userRoles;
    }

} 