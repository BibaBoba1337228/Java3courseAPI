package course.project.API.services;

import course.project.API.models.*;
import course.project.API.repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class PermissionManagementService {

    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionManagementService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public void assignPermissionToUser(User user, String permissionName) {
        Permission permission = getOrCreatePermission(permissionName);
        user.getPermissions().add(permission);
    }

    @Transactional
    public void removePermissionFromUser(User user, String permissionName) {
        Permission permission = permissionRepository.findByName(permissionName);
        if (permission != null) {
            user.getPermissions().remove(permission);
        }
    }

    @Transactional
    public void assignPermissionToProject(Project project, String permissionName) {
        Permission permission = getOrCreatePermission(permissionName);
        project.getPermissions().add(permission);
    }

    @Transactional
    public void removePermissionFromProject(Project project, String permissionName) {
        Permission permission = permissionRepository.findByName(permissionName);
        if (permission != null) {
            project.getPermissions().remove(permission);
        }
    }

    @Transactional
    public void assignPermissionToBoard(Board board, String permissionName) {
        Permission permission = getOrCreatePermission(permissionName);
        board.getPermissions().add(permission);
    }

    @Transactional
    public void removePermissionFromBoard(Board board, String permissionName) {
        Permission permission = permissionRepository.findByName(permissionName);
        if (permission != null) {
            board.getPermissions().remove(permission);
        }
    }

    @Transactional
    public void assignDefaultProjectPermissions(Project project) {
        Set<String> defaultPermissions = new HashSet<>();
        defaultPermissions.add("UPDATE_PROJECT");
        defaultPermissions.add("MANAGE_PROJECT_PARTICIPANTS");
        
        for (String permissionName : defaultPermissions) {
            assignPermissionToProject(project, permissionName);
        }
    }

    @Transactional
    public void assignDefaultBoardPermissions(Board board) {
        Set<String> defaultPermissions = new HashSet<>();
        defaultPermissions.add("UPDATE_BOARD");
        defaultPermissions.add("MANAGE_BOARD_PARTICIPANTS");
        defaultPermissions.add("MANAGE_BOARD_TAGS");
        defaultPermissions.add("CREATE_TASK");
        defaultPermissions.add("UPDATE_TASK");
        defaultPermissions.add("DELETE_TASK");
        defaultPermissions.add("MANAGE_TASK_COLUMN");
        defaultPermissions.add("MOVE_TASK");
        defaultPermissions.add("UPDATE_TASK_STATUS");
        
        for (String permissionName : defaultPermissions) {
            assignPermissionToBoard(board, permissionName);
        }
    }

    private Permission getOrCreatePermission(String name) {
        Permission permission = permissionRepository.findByName(name);
        if (permission == null) {
            permission = new Permission(name, "Permission for " + name);
            permission = permissionRepository.save(permission);
        }
        return permission;
    }
} 