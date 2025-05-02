package course.project.API.repositories;

import course.project.API.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    @EntityGraph(attributePaths = {"participants", "owner"})
    List<Project> findByOwner_IdOrParticipants_Id(Long ownerId, Long participantId);
    boolean existsByOwner_UsernameAndParticipants_Username(String ownerUsername, String participantUsername);
    boolean existsByOwner_Username(String username);
    boolean existsByParticipants_Username(String username);
} 