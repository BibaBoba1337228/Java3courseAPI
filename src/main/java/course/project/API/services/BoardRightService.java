package course.project.API.services;

import course.project.API.models.*;
import course.project.API.repositories.BoardRepository;
import course.project.API.repositories.BoardUserRightRepository;
import course.project.API.repositories.ProjectRepository;
import course.project.API.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BoardRightService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final BoardUserRightRepository boardUserRightRepository;
    private final ProjectRightService projectRightService;

    @Autowired
    public BoardRightService(BoardRepository boardRepository,
                         UserRepository userRepository,
                         ProjectRepository projectRepository,
                         BoardUserRightRepository boardUserRightRepository,
                         ProjectRightService projectRightService) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.boardUserRightRepository = boardUserRightRepository;
        this.projectRightService = projectRightService;
    }

    /**
     * Add a user to a board with basic rights
     */
    @Transactional
    public void addUserToBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        Project project = board.getProject();
        
        // User must be a participant in the project first
        if (!project.getParticipants().contains(user)) {
            throw new RuntimeException("User must be added to the project first");
        }
        
        // Add user as board participant
        board.addParticipant(user);
        
        // By default, give VIEW_BOARD right
        grantBoardRight(boardId, userId, BoardRight.VIEW_BOARD);
        
        boardRepository.save(board);
    }
    
    /**
     * Grant a specific right to a user on a board
     */
    @Transactional
    public void grantBoardRight(Long boardId, Long userId, BoardRight right) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Check if user is board participant
        if (!board.getParticipants().contains(user)) {
            board.addParticipant(user);
        }
        
        // Check if right already exists
        if (boardUserRightRepository.findByBoardAndUserAndRight(board, user, right).isEmpty()) {
            board.addUserRight(user, right);
            boardRepository.save(board);
        }
    }
    
    /**
     * Revoke a specific right from a user on a board
     */
    @Transactional
    public void revokeBoardRight(Long boardId, Long userId, BoardRight right) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Cannot revoke rights from project owner
        if (board.getProject().getOwner().equals(user)) {
            throw new RuntimeException("Cannot revoke rights from project owner");
        }
        
        board.removeUserRight(user, right);
        boardRepository.save(board);
    }
    
    /**
     * Get all rights for a user on a board
     */
    public Set<BoardRight> getUserBoardRights(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Project owner has all rights
        if (board.getProject().getOwner().equals(user)) {
            return new HashSet<>(Arrays.asList(BoardRight.values()));
        }
        
        // Get user's board rights
        List<BoardUserRight> userRights = boardUserRightRepository.findByBoardAndUser(board, user);
        return userRights.stream()
                .map(BoardUserRight::getRight)
                .collect(Collectors.toSet());
    }
    
    /**
     * Check if a user has a specific right on a board
     */
    public boolean hasBoardRight(Long boardId, Long userId, BoardRight right) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        return board.hasRight(user, right);
    }
    
    /**
     * Remove a user from a board
     */
    @Transactional
    public void removeUserFromBoard(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Cannot remove project owner from board
        if (board.getProject().getOwner().equals(user)) {
            throw new RuntimeException("Cannot remove project owner from board");
        }
        
        board.removeParticipant(user);
        boardRepository.save(board);
        
        // Rights will be automatically removed by the cascade
    }
    
    /**
     * Get visible boards for a user in a project
     */
    public List<Board> getVisibleBoardsForUser(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Project owner sees all boards
        if (project.getOwner().equals(user)) {
            return project.getBoards();
        }
        
        // Check if user has VIEW_PROJECT right
        if (!projectRightService.hasProjectRight(projectId, userId, ProjectRight.VIEW_PROJECT)) {
            throw new RuntimeException("User does not have permission to view this project");
        }
        
        // Return only boards where user is a participant with VIEW_BOARD right
        return project.getBoards().stream()
                .filter(board -> board.getParticipants().contains(user) && 
                        hasBoardRight(board.getId(), userId, BoardRight.VIEW_BOARD))
                .collect(Collectors.toList());
    }
} 