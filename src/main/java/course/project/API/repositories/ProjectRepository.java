package course.project.API.repositories;

import course.project.API.models.Project;
import course.project.API.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    @EntityGraph(attributePaths = {"participants", "owner"})
    List<Project> findByOwner_IdOrParticipants_Id(Long ownerId, Long participantId);
    boolean existsByOwner_UsernameAndParticipants_Username(String ownerUsername, String participantUsername);
    boolean existsByOwner_Username(String username);
    boolean existsByParticipants_Username(String username);
    Project findById(long id);
    
    @EntityGraph(attributePaths = {"participants", "owner"})
    Optional<Project> findProjectWithParticipantsOwnerById(Long id);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.participants " +
            "LEFT JOIN FETCH p.owner " +
            "WHERE p.owner.id = :userId OR :userId IN (SELECT part.id FROM p.participants part)")
    List<Project> findProjectsWithUsersAndOwnerByUserId(@Param("userId") Long userId);

    List<Project> findByParticipantsContains(User user);
    
    List<Project> findByOwner(User owner);
}