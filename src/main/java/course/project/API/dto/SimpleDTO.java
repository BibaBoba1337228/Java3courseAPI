package course.project.API.dto;

public class SimpleDTO {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public SimpleDTO(String message) {
        this.message = message;
    }
}
