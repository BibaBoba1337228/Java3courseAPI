package course.project.API.controllers;

import course.project.API.dto.invitation.InvitationDTO;
import course.project.API.models.User;
import course.project.API.services.InvitationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);

    private final InvitationService invitationService;

    @Autowired
    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/send")
    public ResponseEntity<InvitationDTO> sendInvitation(
            @RequestParam Long recipientId,
            @RequestParam Long projectId,
            Authentication authentication) {
        User sender = (User) authentication.getPrincipal();
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
        Long userId = Long.valueOf(authentication.getName());
        try {
            List<InvitationDTO> invitations = invitationService.getPendingInvitations(userId);
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            logger.error("Error getting pending invitations: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 