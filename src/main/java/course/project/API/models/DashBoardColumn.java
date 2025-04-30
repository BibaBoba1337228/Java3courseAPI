package course.project.API.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dashboard_columns")
public class DashBoardColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    @JsonBackReference
    private Board board;
    
    @Transient
    private Long boardId;

    @OneToMany(mappedBy = "column", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Task> tasks = new ArrayList<>();

    @Column
    private Integer position;

    public DashBoardColumn() {
    }

    public DashBoardColumn(String name, Board board, Integer position) {
        this.name = name;
        this.board = board;
        this.position = position;
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

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }
    
    public Long getBoardId() {
        if (board != null) {
            return board.getId();
        }
        return boardId;
    }
    
    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public void addTask(Task task) {
        tasks.add(task);
        task.setColumn(this);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        task.setColumn(null);
    }

    @Override
    public String toString() {
        return "DashBoardColumn{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", boardId=" + (board != null ? board.getId() : boardId) +
                ", position=" + position +
                ", tasks=" + tasks.size() +
                '}';
    }
} 