package course.project.API.dto;

public class RightDto {
    private Long userId;
    private String rightName;

    public RightDto() {
    }

    public RightDto(Long userId, String rightName) {
        this.userId = userId;
        this.rightName = rightName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRightName() {
        return rightName;
    }

    public void setRightName(String rightName) {
        this.rightName = rightName;
    }
} 