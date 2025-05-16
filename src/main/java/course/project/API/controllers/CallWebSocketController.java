package course.project.API.controllers;

import course.project.API.dto.call.CallEventDTO;
import course.project.API.dto.call.CallEventType;
import course.project.API.dto.call.CallType;
import course.project.API.models.ActiveCall;
import course.project.API.models.Chat;
import course.project.API.models.User;
import course.project.API.repositories.ChatRepository;
import course.project.API.services.CallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class CallWebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(CallWebSocketController.class);
    
    private final CallService callService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepository chatRepository;
    
    @Autowired
    public CallWebSocketController(CallService callService, SimpMessagingTemplate messagingTemplate, ChatRepository chatRepository) {
        this.callService = callService;
        this.messagingTemplate = messagingTemplate;
        this.chatRepository = chatRepository;
    }
    
    /**
     * Handles a request to start a call
     */
    @MessageMapping("/call/start")
    public void startCall(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received call start request: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            String callTypeStr = payload.get("callType").toString();
            CallType callType = CallType.valueOf(callTypeStr.toUpperCase());
            
            CallEventDTO callEvent = callService.startCall(chatId, user.getId(), callType);
            
            // If this is a direct chat, immediately send offer to the recipient
            if (callEvent.getType() == CallEventType.OFFER) {
                String sdpOffer = payload.containsKey("sdp") ? payload.get("sdp").toString() : null;
                
                if (sdpOffer != null) {
                    callService.processCallOffer(callEvent, sdpOffer);
                }
            } else {
                // For group chats, broadcast a notification about the call
                messagingTemplate.convertAndSend("/topic/chat/" + chatId, callEvent);
            }
            
        } catch (Exception e) {
            logger.error("Error processing call start: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles call answers (accepting a call)
     */
    @MessageMapping("/call/answer")
    public void answerCall(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received call answer: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long callId = Long.valueOf(payload.get("callId").toString());
            String sdpAnswer = payload.get("sdp").toString();
            
            // Create the call answer event
            CallEventDTO callEvent = new CallEventDTO();
            callEvent.setType(CallEventType.ANSWER);
            callEvent.setChatId(chatId);
            callEvent.setCallId(callId);
            callEvent.setSenderId(user.getId());
            callEvent.setSenderName(user.getName());
            
            // Process the answer through the call service
            callService.processCallAnswer(callEvent, sdpAnswer);
            
        } catch (Exception e) {
            logger.error("Error processing call answer: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles WebRTC ICE candidates
     */
    @MessageMapping("/call/ice-candidate")
    public void handleIceCandidate(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received ICE candidate: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long callId = Long.valueOf(payload.get("callId").toString());
            Object iceCandidate = payload.get("candidate");
            
            // Create the ICE candidate event
            CallEventDTO callEvent = new CallEventDTO();
            callEvent.setType(CallEventType.ICE_CANDIDATE);
            callEvent.setChatId(chatId);
            callEvent.setCallId(callId);
            callEvent.setSenderId(user.getId());
            callEvent.setSenderName(user.getName());
            
            // Process ICE candidate through the call service
            callService.processIceCandidate(callEvent, iceCandidate);
            
        } catch (Exception e) {
            logger.error("Error processing ICE candidate: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles ending a call
     */
    @MessageMapping("/call/end")
    public void endCall(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received call end: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long callId = Long.valueOf(payload.get("callId").toString());
            
            // End the call through the call service
            callService.endCall(chatId, callId, user.getId());
            
        } catch (Exception e) {
            logger.error("Error ending call: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles rejecting a call
     */
    @MessageMapping("/call/reject")
    public void rejectCall(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received call rejection: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long callId = Long.valueOf(payload.get("callId").toString());
            Long initiatorId = Long.valueOf(payload.get("initiatorId").toString());
            
            // Create call rejected event
            CallEventDTO callEvent = new CallEventDTO();
            callEvent.setType(CallEventType.CALL_REJECTED);
            callEvent.setChatId(chatId);
            callEvent.setCallId(callId);
            callEvent.setSenderId(user.getId());
            callEvent.setSenderName(user.getName());
            
            // Send rejection to the initiator
            messagingTemplate.convertAndSendToUser(
                payload.get("initiatorName").toString(),
                "/queue/private",
                callEvent
            );
            
            // Also remove the user from the call participants
            callService.endCall(chatId, callId, user.getId());
            
        } catch (Exception e) {
            logger.error("Error rejecting call: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles inviting a user to join a group call
     */
    @MessageMapping("/call/invite")
    public void inviteToCall(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received call invite request: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long callId = Long.valueOf(payload.get("callId").toString());
            Long inviteeId = Long.valueOf(payload.get("inviteeId").toString());
            
            // Process the invite through the call service
            callService.inviteToCall(chatId, callId, user.getId(), inviteeId);
            
        } catch (Exception e) {
            logger.error("Error inviting to call: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles media status changes (mute, video off, etc.)
     */
    @MessageMapping("/call/media-status")
    public void updateMediaStatus(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received media status update: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long callId = Long.valueOf(payload.get("callId").toString());
            String statusType = payload.get("statusType").toString(); // e.g., "TOGGLE_AUDIO", "TOGGLE_VIDEO"
            Boolean enabled = (Boolean) payload.get("enabled");
            
            // Create media status event
            CallEventDTO callEvent = new CallEventDTO();
            callEvent.setType(CallEventType.valueOf(statusType));
            callEvent.setChatId(chatId);
            callEvent.setCallId(callId);
            callEvent.setSenderId(user.getId());
            callEvent.setSenderName(user.getName());
            callEvent.addToPayload("enabled", enabled);
            
            // Get active call
            ActiveCall activeCall = callService.getActiveCall(chatId);
            if (activeCall != null && activeCall.getId().equals(callId)) {
                if (activeCall.isGroupCall()) {
                    // For group calls, broadcast to all participants
                    messagingTemplate.convertAndSend("/topic/chat/" + chatId, callEvent);
                } else {
                    // For direct chats, send to the other participant
                    Chat chat = chatRepository.findByIdWithParticipants(chatId);
                    if (chat != null) {
                        for (User participant : chat.getParticipants()) {
                            if (!participant.getId().equals(user.getId())) {
                                messagingTemplate.convertAndSendToUser(
                                    participant.getName(),
                                    "/queue/private",
                                    callEvent
                                );
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error updating media status: {}", e.getMessage(), e);
        }
    }
} 