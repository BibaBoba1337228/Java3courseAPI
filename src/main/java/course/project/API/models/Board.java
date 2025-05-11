package course.project.API.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "boards")
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @JsonBackReference
    private Project project;

    @ManyToMany
    @JoinTable(
        name = "board_participants",
        joinColumns = @JoinColumn(name = "board_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DashBoardColumn> columns = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Tag> tags = new ArrayList<>();
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardUserRight> userRights = new ArrayList<>();

    public Board() {
    }

    public Board(String title, String description, Project project) {
        this.title = title;
        this.description = description;
        this.project = project;
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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

    public List<DashBoardColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<DashBoardColumn> columns) {
        this.columns = columns;
    }

    public void addColumn(DashBoardColumn column) {
        columns.add(column);
        column.setBoard(this);
    }

    public void removeColumn(DashBoardColumn column) {
        columns.remove(column);
        column.setBoard(null);
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
        tag.setBoard(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.setBoard(null);
    }
    
    public List<BoardUserRight> getUserRights() {
        return userRights;
    }
    
    public void setUserRights(List<BoardUserRight> userRights) {
        this.userRights = userRights;
    }
    
    public void addUserRight(User user, BoardRight right) {
        BoardUserRight userRight = new BoardUserRight(this, user, right);
        userRights.add(userRight);
    }
    
    public void removeUserRight(User user, BoardRight right) {
        userRights.removeIf(r -> r.getUser().equals(user) && r.getRight() == right);
    }
    
    public boolean hasRight(User user, BoardRight right) {
        // Check if project owner
        if (project.getOwner().equals(user)) {
            System.out.println("hasRight: User is project owner, granting all rights");
            return true; // Project owner has all rights
        }
        
        boolean result = userRights.stream()
                .anyMatch(r -> r.getUser().equals(user) && r.getRight() == right);
        
        System.out.println("hasRight check for user " + user.getUsername() + " and right " + right + ": " + result);
        
        if (!result) {
            // Debug: print all rights this user has
            System.out.println("User rights on this board:");
            userRights.stream()
                .filter(r -> r.getUser().equals(user))
                .forEach(r -> System.out.println("- " + r.getRight()));
        }
        
        return result;
    }
} 