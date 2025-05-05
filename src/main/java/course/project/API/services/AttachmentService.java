package course.project.API.services;

import course.project.API.models.Attachment;
import course.project.API.models.Task;
import course.project.API.repositories.AttachmentRepository;
import course.project.API.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final String uploadDir = "uploads";

    @Autowired
    public AttachmentService(AttachmentRepository attachmentRepository, TaskRepository taskRepository) {
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        
        // Create uploads directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Transactional(readOnly = true)
    public List<Attachment> getAllAttachmentsByTask(Long taskId) {
        return attachmentRepository.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public Optional<Attachment> getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId);
    }

    @Transactional
    public Attachment uploadAttachment(Long taskId, MultipartFile file, String uploadedBy) throws IOException {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Create file path
        Path filePath = Paths.get(uploadDir, uniqueFilename);
        
        // Save file to disk
        Files.write(filePath, file.getBytes());
        
        // Create attachment entity
        Attachment attachment = new Attachment(
            originalFilename,
            filePath.toString(),
            file.getContentType(),
            file.getSize(),
            task,
            uploadedBy
        );
        
        return attachmentRepository.save(attachment);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) throws IOException {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));
        
        // Delete file from disk
        Path filePath = Paths.get(attachment.getFilePath());
        Files.deleteIfExists(filePath);
        
        // Delete from database
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteAllAttachmentsByTask(Long taskId) throws IOException {
        List<Attachment> attachments = attachmentRepository.findByTaskId(taskId);
        
        for (Attachment attachment : attachments) {
            // Delete file from disk
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
        }
        
        // Delete from database
        attachmentRepository.deleteByTaskId(taskId);
    }
} 