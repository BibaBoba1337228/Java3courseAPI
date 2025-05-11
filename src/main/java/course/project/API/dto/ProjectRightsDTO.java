package course.project.API.dto;

import course.project.API.models.ProjectRight;
import java.util.Map;
import java.util.Set;

/**
 * DTO для представления прав пользователя на всех проектах
 */
public class ProjectRightsDTO {
    private Map<Long, Set<ProjectRight>> projectRights;
    
    public ProjectRightsDTO() {
    }
    
    public ProjectRightsDTO(Map<Long, Set<ProjectRight>> projectRights) {
        this.projectRights = projectRights;
    }
    
    public Map<Long, Set<ProjectRight>> getProjectRights() {
        return projectRights;
    }
    
    public void setProjectRights(Map<Long, Set<ProjectRight>> projectRights) {
        this.projectRights = projectRights;
    }
} 