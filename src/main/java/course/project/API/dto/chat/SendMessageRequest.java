package course.project.API.dto.chat;

import java.util.List;

public class SendMessageRequest {
    private String content;
    private List<Long> attachmentIds;

    public SendMessageRequest() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Long> getAttachmentIds() {
        return attachmentIds;
    }

    public void setAttachmentIds(List<Long> attachmentIds) {
        this.attachmentIds = attachmentIds;
    }
} 