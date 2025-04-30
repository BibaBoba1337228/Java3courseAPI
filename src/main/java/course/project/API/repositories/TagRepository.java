package course.project.API.repositories;

import course.project.API.models.Tag;
import course.project.API.models.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    List<Tag> findByBoard(Board board);
    
    List<Tag> findByBoardId(Long boardId);
    
    void deleteByBoardId(Long boardId);
    
    Optional<Tag> findByName(String name);
    
    Optional<Tag> findByNameAndBoardId(String name, Long boardId);
} 