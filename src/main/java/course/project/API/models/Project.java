package course.project.API.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column
    private String emoji;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "project_participants",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<Board> boards = new HashSet<>();
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectUserRight> userRights = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invitation> invitations = new ArrayList<>();

    public Project() {
    }

    public Project(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public Project(String title, String description, User owner) {
        this.title = title;
        this.description = description;
        this.owner = owner;
    }
    
    public Project(String title, String description, String emoji, User owner) {
        this.title = title;
        this.description = description;
        this.emoji = emoji;
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

    public Set<User> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }

    public void addParticipant(User user) {
        participants.add(user);
    }

    public void removeParticipant(User user) {
        participants.remove(user);
        userRights.removeIf(right -> right.getUser().equals(user));
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Set<Board> getBoards() {
        return boards;
    }

    public void setBoards(Set<Board> boards) {
        this.boards = boards;
    }

    public void addBoard(Board board) {
        boards.add(board);
        board.setProject(this);
    }

    public void removeBoard(Board board) {
        boards.remove(board);
        board.setProject(null);
    }
    
    public List<ProjectUserRight> getUserRights() {
        return userRights;
    }
    
    public void setUserRights(List<ProjectUserRight> userRights) {
        this.userRights = userRights;
    }
    
    public void addUserRight(User user, ProjectRight right) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User must not be null");
            }
            if (right == null) {
                throw new IllegalArgumentException("ProjectRight must not be null");
            }
            if (user.getId() == null) {
                throw new IllegalArgumentException("User ID must not be null");
            }
            if (this.getId() == null) {
                throw new IllegalArgumentException("Project ID must not be null");
            }
            
            ProjectUserRight userRight = new ProjectUserRight(this, user, right);
            userRights.add(userRight);
        } catch (Exception e) {
            System.err.println("Error in addUserRight: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void removeUserRight(User user, ProjectRight right) {
        userRights.removeIf(r -> r.getUser().equals(user) && r.getRight() == right);
    }
    
    public boolean hasRight(User user, ProjectRight right) {
        if (user.equals(owner)) {
            return true; // Owner has all rights
        }
        return userRights.stream()
                .anyMatch(r -> r.getUser().equals(user) && r.getRight() == right);
    }

    public List<Invitation> getInvitations() {
        return invitations;
    }
    
    public void setInvitations(List<Invitation> invitations) {
        this.invitations = invitations;
    }
} 