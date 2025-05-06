package course.project.API.models;

public enum BoardRight {
    CREATE_TASKS,        // Permission to create tasks
    EDIT_TASKS,          // Permission to edit tasks
    DELETE_TASKS,        // Permission to delete tasks
    MOVE_TASKS,          // Permission to move tasks between columns and within a column
    MOVE_COLUMNS,        // Permission to reorder columns
    MANAGE_MEMBERS,      // Permission to add/remove users from board
    MANAGE_RIGHTS,       // Permission to edit user rights on board
    VIEW_BOARD           // Basic permission to view the board
} 