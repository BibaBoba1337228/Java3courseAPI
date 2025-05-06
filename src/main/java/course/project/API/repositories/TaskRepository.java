package course.project.API.repositories;

import course.project.API.models.Task;
import course.project.API.models.DashBoardColumn;
import course.project.API.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByColumnOrderByPosition(DashBoardColumn column);
    
    List<Task> findByColumnId(Long columnId);
    
    void deleteByColumnId(Long columnId);
    
    List<Task> findByParticipantsContains(User user);
    
    @Query("SELECT t FROM Task t JOIN t.participants p WHERE p.id = :userId")
    List<Task> findAllTasksByUserId(@Param("userId") Long userId);
} 