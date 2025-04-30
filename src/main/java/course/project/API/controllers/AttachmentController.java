package course.project.API.controllers;

import course.project.API.models.Attachment;
import course.project.API.services.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Autowired
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<Attachment>> getAttachmentsByTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(attachmentService.getAllAttachmentsByTask(taskId));
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Attachment> getAttachmentById(@PathVariable Long attachmentId) {
        return attachmentService.getAttachmentById(attachmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        Attachment attachment = attachmentService.getAttachmentById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));

        Path filePath = Paths.get(attachment.getFilePath());
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    public ResponseEntity<Attachment> uploadAttachment(
            @RequestParam("taskId") Long taskId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy) {
        try {
            Attachment attachment = attachmentService.uploadAttachment(taskId, file, uploadedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        try {
            attachmentService.deleteAttachment(attachmentId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Void> deleteAllAttachmentsByTask(@PathVariable Long taskId) {
        try {
            attachmentService.deleteAllAttachmentsByTask(taskId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 