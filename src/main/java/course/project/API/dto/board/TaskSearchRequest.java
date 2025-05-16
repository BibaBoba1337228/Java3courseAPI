package course.project.API.dto.board;

import java.util.List;

public class TaskSearchRequest {
    private String searchText;
    private Long projectId;
    private Long boardId;
    private Long tagId;
    private Boolean isCompleted;
    private String sortDirection = "asc"; // "asc" or "desc"
    private Boolean isTitleSearch;
    private Boolean isDescriptionSearch;

    public TaskSearchRequest() {
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Boolean getIsTitleSearch() {
        return isTitleSearch;
    }

    public void setIsTitleSearch(Boolean titleSearch) {
        isTitleSearch = titleSearch;
    }

    public Boolean getIsDescriptionSearch() {
        return isDescriptionSearch;
    }

    public void setIsDescriptionSearch(Boolean descriptionSearch) {
        isDescriptionSearch = descriptionSearch;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
} 