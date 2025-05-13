package course.project.API.controllers;

import course.project.API.dto.board.AttachmentDTO;
import course.project.API.models.Attachment;
import course.project.API.services.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import course.project.API.models.User;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final String baseUrl;

    @Autowired
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
        this.baseUrl = "http://localhost:8080"; // Базовый URL для скачивания файлов
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<AttachmentDTO>> getAttachmentsByTask(@PathVariable Long taskId) {
        List<Attachment> attachments = attachmentService.getAllAttachmentsByTask(taskId);
        List<AttachmentDTO> attachmentDTOs = attachments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(attachmentDTOs);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<AttachmentDTO> getAttachmentById(@PathVariable Long attachmentId) {
        return attachmentService.getAttachmentById(attachmentId)
                .map(this::convertToDTO)
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
                .header("Content-Type", attachment.getFileType())
                .body(resource);
    }


    @PostMapping("/upload")
    public ResponseEntity<AttachmentDTO> uploadAttachment(
            @RequestParam("taskId") Long taskId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        try {
            Attachment attachment = attachmentService.uploadAttachment(taskId, file, currentUser.getUsername());
            AttachmentDTO attachmentDTO = convertToDTO(attachment);
            return ResponseEntity.status(HttpStatus.CREATED).body(attachmentDTO);
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
    
    // Helper method to convert Attachment to AttachmentDTO
    private AttachmentDTO convertToDTO(Attachment attachment) {
        AttachmentDTO dto = new AttachmentDTO(
            attachment.getId(),
            attachment.getFileName(),
            null, // Don't expose file path to client
            attachment.getFileType(),
            attachment.getFileSize(),
            attachment.getUploadedBy(),
            attachment.getUploadedAt()
        );
        // Set download URL
        dto.setDownloadUrl(baseUrl + "/api/attachments/" + attachment.getId() + "/download");
        return dto;
    }
} 