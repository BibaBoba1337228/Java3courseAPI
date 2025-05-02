package course.project.API.services;

import course.project.API.models.Tag;
import course.project.API.models.Board;
import course.project.API.repositories.TagRepository;
import course.project.API.repositories.BoardRepository;
import course.project.API.repositories.DashBoardColumnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final BoardRepository boardRepository;
    private final DashBoardColumnRepository columnRepository;

    @Autowired
    public TagService(TagRepository tagRepository, BoardRepository boardRepository, 
                     DashBoardColumnRepository columnRepository) {
        this.tagRepository = tagRepository;
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> getAllTagsByBoard(Long boardId) {
        return tagRepository.findByBoardId(boardId);
    }

    @Transactional(readOnly = true)
    public Optional<Tag> getTagById(Long tagId) {
        return tagRepository.findById(tagId);
    }
    
    @Transactional(readOnly = true)
    public List<Tag> getTagsByColumnId(Long columnId) {
        return columnRepository.findById(columnId)
                .map(column -> tagRepository.findByBoardId(column.getBoard().getId()))
                .orElse(Collections.emptyList());
    }

    @Transactional
    public Tag createTag(Long boardId, String name, String color) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));
        
        Tag tag = new Tag(name, color, board);
        return tagRepository.save(tag);
    }

    @Transactional
    public Tag updateTag(Long tagId, String name, String color) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found with id: " + tagId));
        
        if (name != null) {
            tag.setName(name);
        }
        
        if (color != null) {
            tag.setColor(color);
        }
        
        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Long tagId) {
        tagRepository.deleteById(tagId);
    }

    @Transactional
    public void deleteAllTagsByBoard(Long boardId) {
        tagRepository.deleteByBoardId(boardId);
    }
} 