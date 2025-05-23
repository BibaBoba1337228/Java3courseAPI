package course.project.API.repositories;

import course.project.API.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {



    @Query("""
        SELECT p FROM Project p
        LEFT JOIN FETCH p.participants pp
        WHERE p.ownerId = :userId OR pp.id = :userId
""")
    List<Project> findProjectsByUserId(@Param("userId") Long userId);


    boolean existsByOwner_UsernameAndParticipants_Username(String ownerUsername, String participantUsername);

    boolean existsByOwner_Username(String username);

    boolean existsByParticipants_Username(String username);

    Project findById(long id);

    @EntityGraph(attributePaths = {"participants", "owner"})
    Optional<Project> findProjectWithParticipantsOwnerById(Long id);

    @Modifying
    @Query(value = "INSERT INTO project_participants (project_id, user_id) VALUES (:projectId, :userId)", nativeQuery = true)
    void addUserToProject(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query(value = """
            SELECT p.id, pur.right_name  FROM projects p
            JOIN project_user_rights pur on p.id = pur.project_id
            WHERE (p.owner_id = :userId OR :userId IN (
                SELECT pp.user_id FROM projects p2
                JOIN project_participants pp ON p2.id = pp.project_id
                WHERE pp.user_id = :userId
            )) AND pur.user_id = :userId
            """, nativeQuery = true)
    List<Object[]> findProjectIdAndRightNameByOwnerIdOrParticipantId(@Param("userId") Long userId);

    @Query("""
            SELECT p FROM Project p
            LEFT JOIN FETCH p.boards b 
            LEFT JOIN FETCH b.columns c LEFT JOIN FETCH c.tasks 
            WHERE p.id IN :projectIds
            """)
    List<Project> findProjectsByIdsWithBoardsColumnsTasks(@Param("projectIds") List<Long> projectIds);


}