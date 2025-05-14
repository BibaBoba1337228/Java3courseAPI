package course.project.API.services;

import course.project.API.models.DashBoardColumn;
import course.project.API.models.Board;
import course.project.API.repositories.DashBoardColumnRepository;
import course.project.API.repositories.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DashBoardColumnService {

    private final DashBoardColumnRepository dashBoardColumnRepository;
    private final BoardRepository boardRepository;

    @Autowired
    public DashBoardColumnService(DashBoardColumnRepository dashBoardColumnRepository, BoardRepository boardRepository) {
        this.dashBoardColumnRepository = dashBoardColumnRepository;
        this.boardRepository = boardRepository;
    }

    @Transactional(readOnly = true)
    public List<DashBoardColumn> getAllColumnsByBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        return dashBoardColumnRepository.findByBoardOrderByPosition(board);
    }



    @Transactional(readOnly = true)
    public Optional<DashBoardColumn> getColumnById(Long columnId) {
        return dashBoardColumnRepository.findById(columnId);
    }

    @Transactional
    public DashBoardColumn createColumn(Long boardId, String name, Integer position) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        DashBoardColumn column = new DashBoardColumn(name, board, position);
        return dashBoardColumnRepository.save(column);
    }

    @Transactional
    public DashBoardColumn updateColumn(Long columnId, String name, Integer position) {
        DashBoardColumn column = dashBoardColumnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Column not found with id: " + columnId));
        
        if (name != null) {
            column.setName(name);
        }
        
        if (position != null) {
            column.setPosition(position);
        }
        
        DashBoardColumn savedColumn = dashBoardColumnRepository.save(column);
        dashBoardColumnRepository.flush();
        return savedColumn;
    }

    @Transactional
    public void deleteColumn(Long columnId) {
        dashBoardColumnRepository.deleteById(columnId);
    }

    @Transactional
    public void deleteAllColumnsByBoard(Long boardId) {
        dashBoardColumnRepository.deleteByBoard_Id(boardId);
    }

    @Transactional
    public void updateColumnsPositions(List<Long> columnIds) {
        for (int i = 0; i < columnIds.size(); i++) {
            Long columnId = columnIds.get(i);
            DashBoardColumn column = dashBoardColumnRepository.findById(columnId)
                    .orElseThrow(() -> new RuntimeException("Column not found with id: " + columnId));
            column.setPosition(i);
            dashBoardColumnRepository.save(column);
        }
        dashBoardColumnRepository.flush();
    }
} 