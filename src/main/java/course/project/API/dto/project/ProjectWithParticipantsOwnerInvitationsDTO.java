package course.project.API.dto.project;

import course.project.API.dto.invitation.InvitationWithRecipientDTO;
import course.project.API.dto.user.UserResponse;
import java.util.Set;

public class ProjectWithParticipantsOwnerInvitationsDTO {
    private Long id;
    private String title;
    private String description;
    private Set<UserResponse> participants;
    private UserResponse owner;
    private Set<InvitationWithRecipientDTO> invitations;

    public ProjectWithParticipantsOwnerInvitationsDTO() {
    }

    public ProjectWithParticipantsOwnerInvitationsDTO(Long id, String title, String description, Set<UserResponse> participants, UserResponse owner, Set<InvitationWithRecipientDTO> invitations) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.participants = participants;
        this.owner = owner;
        this.invitations = invitations;
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

    public Set<InvitationWithRecipientDTO> getInvitations() {
        return invitations;
    }

    public void setInvitations(Set<InvitationWithRecipientDTO> invitations) {
        this.invitations = invitations;
    }
}