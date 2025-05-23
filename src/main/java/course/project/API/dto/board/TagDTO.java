package course.project.API.dto.board;

public class TagDTO {
    private Long id;
    private String name;
    private String color;
    private Long boardId;

    public TagDTO() {
    }

    public TagDTO(Long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public TagDTO(Long id, String name, String color, Long boardId) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.boardId = boardId;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }
} 