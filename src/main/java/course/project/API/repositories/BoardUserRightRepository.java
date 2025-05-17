package course.project.API.repositories;

import course.project.API.models.Board;
import course.project.API.models.BoardRight;
import course.project.API.models.BoardUserRight;
import course.project.API.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardUserRightRepository extends JpaRepository<BoardUserRight, Long> {
    List<BoardUserRight> findByBoardAndUser(Board board, User user);

    @Query("""
    SELECT COUNT(bur) > 0
    FROM BoardUserRight bur
    WHERE bur.board.id = :boardId
      AND bur.user.id = :userId
      AND bur.right = :right
""")
    boolean existsByBoardIdAndUserIdAndRight(Long boardId, Long userId, BoardRight right);
} 