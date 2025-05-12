package course.project.API.dto.board;

import course.project.API.dto.user.UserResponse;
import java.util.Set;

public class BoardWithParticipantsDTO extends BoardDTO {
    private Set<UserResponse> participants;

    public BoardWithParticipantsDTO() {
        super();
    }

    public Set<UserResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<UserResponse> participants) {
        this.participants = participants;
    }
} 