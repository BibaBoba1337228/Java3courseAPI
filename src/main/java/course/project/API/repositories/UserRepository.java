package course.project.API.repositories;

import course.project.API.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByName(String name);
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
}