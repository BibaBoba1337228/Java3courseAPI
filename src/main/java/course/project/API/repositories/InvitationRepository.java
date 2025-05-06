package course.project.API.repositories;

import course.project.API.models.Invitation;
import course.project.API.models.InvitationStatus;
import course.project.API.models.Project;
import course.project.API.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    List<Invitation> findByRecipientAndStatus(User recipient, InvitationStatus status);
    List<Invitation> findByRecipient(User recipient);
    boolean existsByRecipientAndProjectAndStatus(User recipient, Project project, InvitationStatus status);
    Optional<Invitation> findByRecipientAndProjectAndStatus(User recipient, Project project, InvitationStatus status);
    List<Invitation> findByProjectAndStatus(Project project, InvitationStatus status);
    List<Invitation> findByProject(Project project);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Invitation i WHERE i.project = ?1")
    void deleteAllByProject(Project project);
} 