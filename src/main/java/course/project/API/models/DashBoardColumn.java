package course.project.API.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    
    @Column(name = "board_id", updatable = false, insertable = false)
    private Long boardId;

    @OneToMany(mappedBy = "column", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<Task> tasks = new HashSet<>();

    @Column
    private Integer position;

    @Column
    private Boolean isCompletionColumn = false;

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

    public Set<Task> getTasks() {
        return tasks;
    }

    public void setTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }

    public Boolean getCompletionColumn() {
        return isCompletionColumn;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public boolean isCompletionColumn() {
        return isCompletionColumn != null && isCompletionColumn;
    }

    public void setCompletionColumn(Boolean completionColumn) {
        isCompletionColumn = completionColumn;
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