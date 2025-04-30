package course.project.API.repositories;

import course.project.API.models.Attachment;
import course.project.API.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    
    List<Attachment> findByTask(Task task);
    
    List<Attachment> findByTaskId(Long taskId);
    
    void deleteByTaskId(Long taskId);
} 