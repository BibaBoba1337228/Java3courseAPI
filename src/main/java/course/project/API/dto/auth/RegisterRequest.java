package course.project.API.dto.auth;

import jakarta.validation.constraints.NotEmpty;

public class RegisterRequest {
    @NotEmpty
    private String username;
    @NotEmpty
    private String password;


    public RegisterRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}