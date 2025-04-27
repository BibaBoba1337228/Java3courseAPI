package course.project.API.repositories;

import course.project.API.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwner_IdOrParticipants_Id(Long ownerId, Long participantId);
} 