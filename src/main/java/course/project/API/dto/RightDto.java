package course.project.API.dto;

public class RightDto {
    private Long userId;
    private String username;
    private String rightName;

    public RightDto() {
    }

    public RightDto(Long userId, String rightName) {
        this.userId = userId;
        this.rightName = rightName;
    }

    public RightDto(String username, String rightName) {
        this.username = username;
        this.rightName = rightName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRightName() {
        return rightName;
    }

    public void setRightName(String rightName) {
        this.rightName = rightName;
    }
} 