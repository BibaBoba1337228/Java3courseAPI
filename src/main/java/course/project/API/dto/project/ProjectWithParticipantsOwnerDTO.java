package course.project.API.dto.project;

import course.project.API.dto.user.UserResponse;
import java.util.Set;

public class ProjectWithParticipantsOwnerDTO {
    private Long id;
    private String title;
    private String description;
    private String emoji;
    private Set<UserResponse> participants;
    private UserResponse owner;

    public ProjectWithParticipantsOwnerDTO() {
    }

    public ProjectWithParticipantsOwnerDTO(Long id, String title, String description, String emoji, Set<UserResponse> participants, UserResponse owner) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.emoji = emoji;
        this.participants = participants;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Set<UserResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<UserResponse> participants) {
        this.participants = participants;
    }

    public UserResponse getOwner() {
        return owner;
    }

    public void setOwner(UserResponse owner) {
        this.owner = owner;
    }
} 