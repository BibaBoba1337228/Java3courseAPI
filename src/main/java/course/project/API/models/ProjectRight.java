package course.project.API.models;

/**
 * Rights that can be granted to users in a project
 */
public enum ProjectRight {
    CREATE_BOARDS,    // Permission to create boards
    EDIT_BOARDS,      // Permission to edit boards  
    DELETE_BOARDS,    // Permission to delete boards
    MANAGE_MEMBERS,   // Permission to add/remove users from project
    MANAGE_RIGHTS,    // Permission to edit user rights
    MANAGE_ACCESS,    // Permission to edit user access to boards (shorter name)
    VIEW_PROJECT,     // Basic permission to view the project
    EDIT_PROJECT,     // Permission to edit project details
    ACCESS_ALL_BOARDS,// Marker for users who should have access to all boards automatically
    MANAGE_BOARD_RIGHTS // Permission to edit user rights on board level
} 