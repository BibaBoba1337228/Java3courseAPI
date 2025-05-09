package course.project.API.controllers;

import course.project.API.dto.SimpleDTO;
import course.project.API.dto.invitation.InvitationDTO;
import course.project.API.dto.invitation.InvitationWithRecipientDTO;
import course.project.API.models.Invitation;
import course.project.API.models.ProjectRight;
import course.project.API.models.User;
import course.project.API.repositories.InvitationRepository;
import course.project.API.services.InvitationService;
import course.project.API.services.ProjectRightService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);

    private final InvitationService invitationService;
    private final ProjectRightService projectRightService;
    private final InvitationRepository invitationRepository;

    @Autowired
    public InvitationController(InvitationService invitationService, ProjectRightService projectRightService, InvitationRepository invitationRepository) {
        this.invitationService = invitationService;
        this.projectRightService = projectRightService;
        this.invitationRepository = invitationRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<InvitationDTO> sendInvitation(
            @RequestParam Long recipientId,
            @RequestParam Long projectId,
            Authentication authentication) {
        
        User sender = (User) authentication.getPrincipal();
        
        // Проверка права на управление участниками проекта
        if (!projectRightService.hasProjectRight(projectId, sender.getId(), ProjectRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(null);
        }
        
        try {
            InvitationDTO invitation = invitationService.sendInvitation(sender.getId(), recipientId, projectId);
            return ResponseEntity.ok(invitation);
        } catch (Exception e) {
            logger.error("Error sending invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<InvitationDTO> acceptInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        try {
            InvitationDTO invitation = invitationService.acceptInvitation(invitationId, user.getId());
            return ResponseEntity.ok(invitation);
        } catch (Exception e) {
            logger.error("Error accepting invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<InvitationDTO> rejectInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        try {
            InvitationDTO invitation = invitationService.rejectInvitation(invitationId, user.getId());
            return ResponseEntity.ok(invitation);
        } catch (Exception e) {
            logger.error("Error rejecting invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{invitationId}/cancel")
    public ResponseEntity<SimpleDTO> cancelInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        try {
            Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
            
            Long projectId = invitation.getProject().getId();
            
            if (!projectRightService.hasProjectRight(projectId, user.getId(), ProjectRight.MANAGE_MEMBERS)) {
                logger.warn("User {} does not have MANAGE_MEMBERS right for project {}", user.getId(), projectId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (invitationService.cancelInvitation(invitationId)){
                return ResponseEntity.ok().body(new SimpleDTO("Invitation cancelled successfully"));
            }
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            logger.error("Invitation not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new SimpleDTO("Invitation not found"));
        } catch (Exception e) {
            logger.error("Error cancelling invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    

    @GetMapping("/my")
    public ResponseEntity<List<InvitationDTO>> getMyInvitations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        try {
            List<InvitationDTO> invitations = invitationService.getUserInvitations(user.getId());
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            logger.error("Error getting user invitations: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my/pending")
    public ResponseEntity<List<InvitationDTO>> getMyPendingInvitations(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        try {
            List<InvitationDTO> invitations = invitationService.getPendingInvitations(user.getId());
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            logger.error("Error getting pending invitations: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<InvitationWithRecipientDTO>> getInvitationsForProject(
            Authentication authentication, 
            @PathVariable Long projectId) {
        
        User currentUser = (User) authentication.getPrincipal();
        
        if (!projectRightService.hasProjectRight(projectId, currentUser.getId(), ProjectRight.MANAGE_MEMBERS)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            List<InvitationWithRecipientDTO> invitations = invitationService.getInvitationsForProject(projectId, currentUser.getId());
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            logger.error("Error getting project invitations: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 