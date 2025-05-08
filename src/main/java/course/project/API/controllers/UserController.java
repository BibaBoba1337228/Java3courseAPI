package course.project.API.controllers;

import course.project.API.dto.user.UserResponse;
import course.project.API.models.User;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public UserController(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(new UserResponse(
                user.getId(),
                user.getName(),
                user.getAvatarURL()
        ));
    }

    @GetMapping("/{username}")
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
            return ResponseEntity.ok(new UserResponse(user.getId(), user.getName(), user.getAvatarURL()));
        }
        // Проверка: есть ли общий проект
        boolean allowed = projectRepository.existsByOwner_UsernameAndParticipants_Username(currentUsername, username)
                || projectRepository.existsByOwner_UsernameAndParticipants_Username(username, currentUsername)
                || projectRepository.existsByOwner_Username(currentUsername) && username.equals(currentUsername)
                || projectRepository.existsByParticipants_Username(currentUsername) && username.equals(currentUsername);
        if (!allowed) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getName(), user.getAvatarURL()));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userRepository.findByNameContainingIgnoreCase(name, pageable);
            
            List<UserResponse> userResponses = users.getContent().stream()
                .map(user -> new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getAvatarURL()
                ))
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", userResponses);
            response.put("hasNext", users.hasNext());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching users: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
