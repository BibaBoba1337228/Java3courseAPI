package course.project.API.repositories;

import course.project.API.models.Project;
import course.project.API.models.ProjectRight;
import course.project.API.models.ProjectUserRight;
import course.project.API.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectUserRightRepository extends JpaRepository<ProjectUserRight, Long> {
    List<ProjectUserRight> findByProject(Project project);
    List<ProjectUserRight> findByUser(User user);
    List<ProjectUserRight> findByProjectAndUser(Project project, User user);
    Optional<ProjectUserRight> findByProjectAndUserAndRight(Project project, User user, ProjectRight right);
} 