package course.project.API.services;

import course.project.API.models.User;
import course.project.API.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String rawPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        String name = "";
        boolean flag = true;
        while (flag) {
            name = "user" + (int) (Math.random() * 1000000);
            flag = userRepository.existsByName(name);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        User newUser = new User(username, encodedPassword, name);
        return userRepository.save(newUser);
    }
}
