package course.project.API.services;

import course.project.API.models.*;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.BoardRepository;
import course.project.API.repositories.TaskRepository;
import course.project.API.repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class PermissionService {

    private final ProjectRepository projectRepository;
    private final BoardRepository boardRepository;
    private final TaskRepository taskRepository;
    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionService(ProjectRepository projectRepository, 
                           BoardRepository boardRepository,
                           TaskRepository taskRepository,
                           PermissionRepository permissionRepository) {
        this.projectRepository = projectRepository;
        this.boardRepository = boardRepository;
        this.taskRepository = taskRepository;
        this.permissionRepository = permissionRepository;
    }

    // Project permissions
    public boolean canCreateProject(Long userId) {
        return true; // Any user can create a project
    }

    public boolean canUpdateProject(Long userId, Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) return false;
        
        Project project = projectOpt.get();
        return project.getOwner().getId().equals(userId) || 
               project.getParticipants().stream().anyMatch(p -> p.getId().equals(userId));
    }

    public boolean canDeleteProject(Long userId, Long projectId) {
        return isProjectOwner(userId, projectId);
    }

    public boolean canManageProjectParticipants(Long userId, Long projectId, Long targetUserId) {
        if (userId.equals(targetUserId)) return true;
        if (isProjectOwner(userId, projectId)) return true;
        
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) return false;
        
        Project project = projectOpt.get();
        return project.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
               !project.getOwner().getId().equals(targetUserId);
    }

    // Board permissions
    public boolean canCreateBoard(Long userId, Long projectId) {
        return hasProjectAccess(userId, projectId);
    }

    public boolean canUpdateBoard(Long userId, Long boardId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isEmpty()) return false;
        
        Board board = boardOpt.get();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("UPDATE_BOARD"));
    }

    public boolean canDeleteBoard(Long userId, Long boardId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isEmpty()) return false;
        
        return hasProjectAccess(userId, boardOpt.get().getProject().getId());
    }

    public boolean canManageBoardParticipants(Long userId, Long boardId, Long targetUserId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isEmpty()) return false;
        
        Board board = boardOpt.get();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("MANAGE_BOARD_PARTICIPANTS"));
    }

    public boolean canManageBoardTags(Long userId, Long boardId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isEmpty()) return false;
        
        Board board = boardOpt.get();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("MANAGE_BOARD_TAGS"));
    }

    // Task permissions
    public boolean canCreateTask(Long userId, Long boardId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isEmpty()) return false;
        
        Board board = boardOpt.get();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("CREATE_TASK"));
    }

    public boolean canUpdateTask(Long userId, Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;
        
        Task task = taskOpt.get();
        Board board = task.getColumn().getBoard();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("UPDATE_TASK"));
    }

    public boolean canDeleteTask(Long userId, Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;
        
        Task task = taskOpt.get();
        Board board = task.getColumn().getBoard();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("DELETE_TASK"));
    }

    public boolean canManageTaskColumn(Long userId, Long boardId) {
        Optional<Board> boardOpt = boardRepository.findById(boardId);
        if (boardOpt.isEmpty()) return false;
        
        Board board = boardOpt.get();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("MANAGE_TASK_COLUMN"));
    }

    public boolean canMoveTask(Long userId, Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;
        
        Task task = taskOpt.get();
        Board board = task.getColumn().getBoard();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("MOVE_TASK"));
    }

    public boolean canUpdateTaskStatus(Long userId, Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;
        
        Task task = taskOpt.get();
        Board board = task.getColumn().getBoard();
        return hasProjectAccess(userId, board.getProject().getId()) ||
               (board.getParticipants().stream().anyMatch(p -> p.getId().equals(userId)) &&
                board.hasPermission("UPDATE_TASK_STATUS"));
    }

    public boolean canUpdateChecklistItem(Long userId, Long taskId) {
        return true; // Any user can update checklist items
    }

    // Helper methods
    private boolean hasProjectAccess(Long userId, Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) return false;
        
        Project project = projectOpt.get();
        return project.getOwner().getId().equals(userId) || 
               project.getParticipants().stream().anyMatch(p -> p.getId().equals(userId));
    }

    private boolean isProjectOwner(Long userId, Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) return false;
        
        return projectOpt.get().getOwner().getId().equals(userId);
    }
} 