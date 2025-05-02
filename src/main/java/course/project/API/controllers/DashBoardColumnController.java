package course.project.API.controllers;

import course.project.API.models.DashBoardColumn;
import course.project.API.services.DashBoardColumnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/columns")
public class DashBoardColumnController {

    private final DashBoardColumnService columnService;

    @Autowired
    public DashBoardColumnController(DashBoardColumnService columnService) {
        this.columnService = columnService;
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<DashBoardColumn>> getColumnsByBoard(@PathVariable Long boardId) {
        List<DashBoardColumn> columns = columnService.getAllColumnsByBoard(boardId);
        // Добавим информацию о boardId для фронтенда
        columns.forEach(column -> {
            if (column.getBoard() != null) {
                column.setBoardId(column.getBoard().getId());
            }
        });
        return ResponseEntity.ok(columns);
    }

    @GetMapping("/{columnId}")
    public ResponseEntity<DashBoardColumn> getColumnById(@PathVariable Long columnId) {
        return columnService.getColumnById(columnId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DashBoardColumn> createColumn(@RequestBody Map<String, Object> payload) {
        Long boardId = Long.parseLong(payload.get("boardId").toString());
        String title = payload.get("title").toString();
        Integer position = payload.containsKey("position") ? 
                Integer.parseInt(payload.get("position").toString()) : 0;

        DashBoardColumn column = columnService.createColumn(boardId, title, position);
        return ResponseEntity.status(HttpStatus.CREATED).body(column);
    }

    @PutMapping("/{columnId}")
    public ResponseEntity<DashBoardColumn> updateColumn(
            @PathVariable Long columnId,
            @RequestBody Map<String, Object> payload) {
        
        System.out.println("Updating column with ID: " + columnId);
        System.out.println("Payload received: " + payload);
        
        String name = payload.containsKey("name") ? payload.get("name").toString() : null;
        Integer position = payload.containsKey("position") ? 
                Integer.parseInt(payload.get("position").toString()) : null;

        System.out.println("Name to update: " + name);
        System.out.println("Position to update: " + position);
        
        DashBoardColumn column = columnService.updateColumn(columnId, name, position);
        System.out.println("Updated column: " + column.getName() + ", position: " + column.getPosition());
        
        return ResponseEntity.ok(column);
    }

    @DeleteMapping("/{columnId}")
    public ResponseEntity<Void> deleteColumn(@PathVariable Long columnId) {
        columnService.deleteColumn(columnId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/board/{boardId}")
    public ResponseEntity<Void> deleteAllColumnsByBoard(@PathVariable Long boardId) {
        columnService.deleteAllColumnsByBoard(boardId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderColumns(@RequestBody List<Long> columnIds) {
        columnService.updateColumnsPositions(columnIds);
        return ResponseEntity.ok().build();
    }
} 