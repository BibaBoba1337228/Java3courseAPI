package course.project.API.repositories;

import course.project.API.models.Board;
import course.project.API.models.BoardRight;
import course.project.API.models.BoardUserRight;
import course.project.API.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardUserRightRepository extends JpaRepository<BoardUserRight, Long> {
    List<BoardUserRight> findByBoard(Board board);
    List<BoardUserRight> findByUser(User user);
    List<BoardUserRight> findByBoardAndUser(Board board, User user);
    Optional<BoardUserRight> findByBoardAndUserAndRight(Board board, User user, BoardRight right);
} 