package course.project.API.dto.chat;

import course.project.API.models.ChatRole;
import course.project.API.models.User;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class ChatDTO {
    private Long id;
    private String name;
    private boolean isGroupChat;
    private String avatarURL;
    private Set<User> participants;
    private Map<User, ChatRole> userRoles;

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

    public void setGroupChat(boolean groupChat) {
        isGroupChat = groupChat;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public Set<User> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }

    public Map<User, ChatRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Map<User, ChatRole> userRoles) {
        this.userRoles = userRoles;
    }

} 