package course.project.API.dto.board;

import course.project.API.dto.user.UserResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private Long columnId;
    private String columnName;
    private Long boardId;
    private String boardTitle;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Set<UserResponse> participants;
    private Integer position;
    private TagDTO tag;
    private List<ChecklistItemDTO> checklist;
    private List<AttachmentDTO> attachments;
    private Long chatId;
    
    public TaskDTO() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getColumnId() {
        return columnId;
    }
    
    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    
    public Long getBoardId() {
        return boardId;
    }
    
    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }
    
    public String getBoardTitle() {
        return boardTitle;
    }
    
    public void setBoardTitle(String boardTitle) {
        this.boardTitle = boardTitle;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }

    public Set<UserResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<UserResponse> participants) {
        this.participants = participants;
    }

    public TagDTO getTag() {
        return tag;
    }

    public void setTag(TagDTO tag) {
        this.tag = tag;
    }

    public List<ChecklistItemDTO> getChecklist() {
        return checklist;
    }
    
    public void setChecklist(List<ChecklistItemDTO> checklist) {
        this.checklist = checklist;
    }

    public List<AttachmentDTO> getAttachments() {
        return attachments;
    }
    
    public void setAttachments(List<AttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}