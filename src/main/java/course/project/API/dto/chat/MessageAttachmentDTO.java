package course.project.API.dto.chat;

public class MessageAttachmentDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private String fileSize;
    private String downloadURL;

    public MessageAttachmentDTO() {}

    public MessageAttachmentDTO(Long id, String fileName, String fileType, String fileSize, String downloadUrl) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.downloadURL = downloadUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public void setDownloadURL(String downloadUrl) {
        this.downloadURL = downloadUrl;
    }
}
