package course.project.API.repositories;

import course.project.API.models.Task;
import course.project.API.models.DashBoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByColumnOrderByPosition(DashBoardColumn column);
    
    List<Task> findByColumnId(Long columnId);
    
    void deleteByColumnId(Long columnId);

} 