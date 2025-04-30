package course.project.API.controllers;

import course.project.API.models.Tag;
import course.project.API.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    @Autowired
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Tag>> getTagsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(tagService.getAllTagsByBoard(boardId));
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<Tag> getTagById(@PathVariable Long tagId) {
        return tagService.getTagById(tagId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tag> createTag(@RequestBody Map<String, Object> payload) {
        Long boardId = Long.parseLong(payload.get("boardId").toString());
        String name = payload.get("name").toString();
        String color = payload.containsKey("color") ? 
                payload.get("color").toString() : "#CCCCCC";

        Tag tag = tagService.createTag(boardId, name, color);
        return ResponseEntity.status(HttpStatus.CREATED).body(tag);
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<Tag> updateTag(
            @PathVariable Long tagId,
            @RequestBody Map<String, Object> payload) {
        
        String name = payload.containsKey("name") ? payload.get("name").toString() : null;
        String color = payload.containsKey("color") ? payload.get("color").toString() : null;

        Tag tag = tagService.updateTag(tagId, name, color);
        return ResponseEntity.ok(tag);
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/board/{boardId}")
    public ResponseEntity<Void> deleteAllTagsByBoard(@PathVariable Long boardId) {
        tagService.deleteAllTagsByBoard(boardId);
        return ResponseEntity.noContent().build();
    }
} 