package course.project.API.dto.board;

public class ChecklistItemDTO {
    private Long id;
    private String text;
    private boolean completed;
    private Integer position;

    public ChecklistItemDTO() {
    }

    public ChecklistItemDTO(Long id, String text, boolean completed, Integer position) {
        this.id = id;
        this.text = text;
        this.completed = completed;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
} 