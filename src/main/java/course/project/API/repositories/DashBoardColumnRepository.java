package course.project.API.repositories;

import course.project.API.models.DashBoardColumn;
import course.project.API.models.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashBoardColumnRepository extends JpaRepository<DashBoardColumn, Long> {
    
    List<DashBoardColumn> findByBoardOrderByPosition(Board board);
    
    List<DashBoardColumn> findByBoard_Id(Long boardId);
    
    void deleteByBoard_Id(Long boardId);
} 