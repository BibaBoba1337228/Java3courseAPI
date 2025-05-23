package course.project.API.repositories;

import course.project.API.models.Task;
import course.project.API.models.DashBoardColumn;
import course.project.API.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByColumnOrderByPosition(DashBoardColumn column);

    List<Task> findByColumnId(Long columnId);

    void deleteByColumnId(Long columnId);

    @EntityGraph(attributePaths = {"participants"})
    List<Task> findWithParticipantsByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"checklist"})
    List<Task> findWithCheckListByIdIn(List<Long> ids);

    @Query("SELECT t FROM Task t JOIN t.participants p WHERE p.id = :userId")
    List<Task> findAllTasksByUserId(@Param("userId") Long userId);


    @Query(value = """
            SELECT t.id, t.column_id, t.title, t.description, t.start_date, t.end_date, tag.id, tag.name, tag.color, u.id, u.name, u.avatarurl FROM projects p
                       JOIN boards b ON p.id = b.project_id
                       JOIN dashboard_columns c ON b.id = c.board_id 
                       JOIN tasks t ON c.id = t.column_id 
                       LEFT JOIN tags tag ON t.tag_id = tag.id
                       LEFT JOIN task_participants tp ON t.id = tp.task_id
                       JOIN users u ON tp.user_id = u.id
                       WHERE (p.owner_id = :userId OR :userId IN (
                            SELECT pp.user_id FROM projects p2
                            JOIN project_participants pp ON p2.id = pp.project_id
                            WHERE pp.user_id = :userId
                        ))
                       AND ( :searchText IS NULL OR (:isTitleSearchExcluded = TRUE OR LOWER(t.title) LIKE LOWER(CONCAT('%', :searchText, '%'))) OR 
                               (:isDescriptionSearchExcluded = TRUE OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchText, '%')))) 
                       AND (:projectId IS NULL OR p.id = :projectId) 
                       AND (:boardId IS NULL OR b.id = :boardId) 
                       AND (:tagId IS NULL OR tag.id = :tagId) 
                       AND (:isCompleted IS NULL OR c.is_completion_column = :isCompleted) 
                       ORDER BY t.end_date ASC
            """, nativeQuery = true)
    List<Object[]> searchTasksAsc(
            @Param("searchText") String searchText,
            @Param("projectId") Long projectId,
            @Param("boardId") Long boardId,
            @Param("tagId") Long tagId,
            @Param("isCompleted") Boolean isCompleted,
            @Param("isTitleSearchExcluded") Boolean isTitleSearchExcluded,
            @Param("isDescriptionSearchExcluded") Boolean isDescriptionSearchExcluded,
            @Param("userId") Long userId
    );

    @Query(value = """
            SELECT t.id, t.column_id, t.title, t.description, t.start_date, t.end_date, tag.id, tag.name, tag.color, u.id, u.name, u.avatarurl FROM projects p
                       JOIN boards b ON p.id = b.project_id
                       JOIN dashboard_columns c ON b.id = c.board_id 
                       JOIN tasks t ON c.id = t.column_id 
                       LEFT JOIN tags tag ON t.tag_id = tag.id
                       LEFT JOIN task_participants tp ON t.id = tp.task_id
                       JOIN users u ON tp.user_id = u.id
                       WHERE (p.owner_id = :userId OR :userId IN (
                            SELECT pp.user_id FROM projects p2
                            JOIN project_participants pp ON p2.id = pp.project_id
                            WHERE pp.user_id = :userId
                        ))
                       AND ( :searchText IS NULL OR (:isTitleSearchExcluded = TRUE OR LOWER(t.title) LIKE LOWER(CONCAT('%', :searchText, '%'))) OR 
                               (:isDescriptionSearchExcluded = TRUE OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchText, '%')))) 
                       AND (:projectId IS NULL OR p.id = :projectId) 
                       AND (:boardId IS NULL OR b.id = :boardId) 
                       AND (:tagId IS NULL OR tag.id = :tagId) 
                       AND (:isCompleted IS NULL OR c.is_completion_column = :isCompleted) 
                       ORDER BY t.end_date DESC 
            """, nativeQuery = true)
    List<Object[]> searchTasksDesc(
            @Param("searchText") String searchText,
            @Param("projectId") Long projectId,
            @Param("boardId") Long boardId,
            @Param("tagId") Long tagId,
            @Param("isCompleted") Boolean isCompleted,
            @Param("isTitleSearchExcluded") Boolean isTitleSearchExcluded,
            @Param("isDescriptionSearchExcluded") Boolean isDescriptionSearchExcluded,
            @Param("userId") Long userId);
} 