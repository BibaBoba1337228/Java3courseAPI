package course.project.API.models;

import jakarta.persistence.*;

@Entity
@Table(name = "project_user_rights",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id", "right_name"}))
public class ProjectUserRight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "right_name", nullable = false)
    private ProjectRight right;

    public ProjectUserRight() {
    }

    public ProjectUserRight(Project project, User user, ProjectRight right) {
        this.project = project;
        this.user = user;
        this.right = right;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ProjectRight getRight() {
        return right;
    }

    public void setRight(ProjectRight right) {
        this.right = right;
    }
} 