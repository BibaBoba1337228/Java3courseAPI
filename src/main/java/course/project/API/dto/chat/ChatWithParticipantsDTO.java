package course.project.API.dto.chat;

import java.util.List;

public class ChatWithParticipantsDTO {
    private Long id;
    private String name;
    private boolean isGroupChat;
    private String avatarURL;
    private List<ParticipantDTO> participants;

    public ChatWithParticipantsDTO() {
    }

    public ChatWithParticipantsDTO(Long id, String name, boolean isGroupChat, String avatarURL, List<ParticipantDTO> participants) {
        this.id = id;
        this.name = name;
        this.isGroupChat = isGroupChat;
        this.avatarURL = avatarURL;
        this.participants = participants;
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

    public List<ParticipantDTO> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantDTO> participants) {
        this.participants = participants;
    }
} 