package course.project.API.models;

import jakarta.persistence.*;

@Entity
@Table(name = "board_user_rights",
       uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_id", "right_name"}))
public class BoardUserRight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "right_name", nullable = false)
    private BoardRight right;

    public BoardUserRight() {
    }

    public BoardUserRight(Board board, User user, BoardRight right) {
        this.board = board;
        this.user = user;
        this.right = right;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BoardRight getRight() {
        return right;
    }

    public void setRight(BoardRight right) {
        this.right = right;
    }
} 