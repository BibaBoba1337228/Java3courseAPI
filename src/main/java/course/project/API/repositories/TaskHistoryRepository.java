package course.project.API.repositories;

import course.project.API.models.TaskHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    
    List<TaskHistory> findByTaskIdOrderByTimestampDesc(Long taskId);

    @EntityGraph(attributePaths = {"user"})
    List<TaskHistory> findByBoardIdOrderByTimestampDesc(Long boardId);
    
    @Query("SELECT th FROM TaskHistory th WHERE th.boardId = :boardId ORDER BY th.timestamp DESC")
    List<TaskHistory> findTaskHistoryByBoardId(@Param("boardId") Long boardId);
} 