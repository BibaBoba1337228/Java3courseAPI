package course.project.API.repositories;

import course.project.API.models.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByProjectId(Long projectId);

    @Query("""
    SELECT DISTINCT b FROM Board b
    LEFT JOIN FETCH b.columns c
    LEFT JOIN FETCH c.tasks t
    LEFT JOIN FETCH t.attachments
    WHERE b.id = :id
    """)
    Board findWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"participants"})
    List<Board> findWithParticipantsByProjectId(Long projectId);


    @EntityGraph(attributePaths = {"participants"})
    Board findWithParticipantsAndOwnerById(Long id);
}