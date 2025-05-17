package course.project.API.repositories;

import course.project.API.models.Project;
import course.project.API.models.ProjectRight;
import course.project.API.models.ProjectUserRight;
import course.project.API.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectUserRightRepository extends JpaRepository<ProjectUserRight, Long> {
    List<ProjectUserRight> findByProject(Project project);
    Optional<ProjectUserRight> findByProjectAndUserAndRight(Project project, User user, ProjectRight right);

    @EntityGraph(attributePaths = {"right"})
    List<ProjectUserRight> findByProjectIdAndUserId(Long projectId, Long userId);


    @Modifying
    @Query(value = "INSERT INTO project_user_rights (right_name, project_id, user_id) VALUES (:rightName, :projectIdd, :userId)", nativeQuery = true)
    void addUserProjectRight(@Param("projectId") Long projectId, @Param("userId") Long userId, @Param("projectRightName") String projectRightName);

    boolean existsByProjectIdAndUserIdAndRight(Long projectId, Long userId, ProjectRight right);

} 