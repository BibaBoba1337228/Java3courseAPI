package course.project.API.dto.project;

public class ProjectDTO {
    private Long id;
    private String title;
    private String description;
    private String emoji;

    public ProjectDTO() {
    }

    public ProjectDTO(Long id, String title, String description, String emoji) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.emoji = emoji;
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
}
