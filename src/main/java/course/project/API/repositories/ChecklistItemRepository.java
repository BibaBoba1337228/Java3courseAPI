package course.project.API.repositories;

import course.project.API.models.ChecklistItem;
import course.project.API.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    
    List<ChecklistItem> findByTaskOrderByPosition(Task task);
    
    List<ChecklistItem> findByTaskId(Long taskId);
    
    void deleteByTaskId(Long taskId);
} 