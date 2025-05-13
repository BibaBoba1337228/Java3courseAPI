package course.project.API.dto.chat;

import course.project.API.models.ChatRole;

public class ParticipantDTO {
    private Long id;
    private String name;
    private String avatarURL;
    private ChatRole role;

    public ParticipantDTO() {
    }

    public ParticipantDTO(Long id, String name, String avatarURL, ChatRole role) {
        this.id = id;
        this.name = name;
        this.avatarURL = avatarURL;
        this.role = role;
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

    public ChatRole getRole() {
        return role;
    }

    public void setRole(ChatRole role) {
        this.role = role;
    }
} 