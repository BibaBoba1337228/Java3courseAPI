package course.project.API.controllers;

import course.project.API.dto.auth.ErrorResponse;
import course.project.API.dto.auth.LoginRequest;
import course.project.API.dto.auth.RegisterRequest;
import course.project.API.dto.auth.UserResponse;
import course.project.API.models.User;
import course.project.API.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import course.project.API.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.security.Principal;
import course.project.API.repositories.ProjectRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest req) {
        try {
            User registered = authService.register(
                request.getUsername(), 
                request.getPassword()
            );
            
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            req.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            
            return ResponseEntity.ok(new UserResponse(
                registered.getUsername(),
                registered.getName(),
                registered.getAvatarURL()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Registration failed", "Username already exists"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest req) {
        try {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Login failed", "Username is required"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Login failed", "Password is required"));
            }

            logger.info("Attempting login for user: {}", request.getUsername());
            
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            req.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            User user = (User) auth.getPrincipal();
            logger.info("Login successful for user: {}", user.getUsername());
            
            return ResponseEntity.ok(new UserResponse(
                user.getUsername(),
                user.getName(),
                user.getAvatarURL()
            ));
        } catch (Exception e) {
            logger.error("Login failed for user: {} - Error: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Login failed", "Invalid username or password"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            User user = (User) auth.getPrincipal();
            return ResponseEntity.ok(new UserResponse(
                user.getUsername(),
                user.getName(),
                user.getAvatarURL()
            ));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Authentication failed", "Not authenticated"));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserResponse profile, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            User user = (User) auth.getPrincipal();
            user.setName(profile.getName());
            user.setAvatarURL(profile.getAvatarURL());
            User updatedUser = authService.updateUser(user);
            return ResponseEntity.ok(new UserResponse(
                updatedUser.getUsername(),
                updatedUser.getName(),
                updatedUser.getAvatarURL()
            ));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Authentication failed", "Not authenticated"));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(
        @PathVariable String username,
        Principal principal
    ) {
        String currentUsername = principal.getName();
        logger.info("currentUsername: {}, requested: {}", currentUsername, username);
        logger.info("isOwner: {}", projectRepository.existsByOwner_Username(currentUsername));
        logger.info("isParticipant: {}", projectRepository.existsByParticipants_Username(currentUsername));
        var userOpt = userRepository.findByUsername(username).or(() -> userRepository.findByName(username));
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var user = userOpt.get();
        // Если сам себя — всегда можно
        if (currentUsername.equals(username) || currentUsername.equals(user.getName())) {
            return ResponseEntity.ok(new UserResponse(user.getUsername(), user.getName(), user.getAvatarURL()));
        }
        // Проверка: есть ли общий проект
        boolean allowed = projectRepository.existsByOwner_UsernameAndParticipants_Username(currentUsername, username)
            || projectRepository.existsByOwner_UsernameAndParticipants_Username(username, currentUsername)
            || projectRepository.existsByOwner_Username(currentUsername) && username.equals(currentUsername)
            || projectRepository.existsByParticipants_Username(currentUsername) && username.equals(currentUsername);
        if (!allowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(new UserResponse(user.getUsername(), user.getName(), user.getAvatarURL()));
    }
}