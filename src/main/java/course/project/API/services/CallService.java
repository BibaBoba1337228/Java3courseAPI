package course.project.API.services;

import course.project.API.dto.call.CallEventDTO;
import course.project.API.dto.call.CallEventType;
import course.project.API.dto.call.CallType;
import course.project.API.models.ActiveCall;
import course.project.API.models.Chat;
import course.project.API.models.User;
import course.project.API.repositories.ChatRepository;
import course.project.API.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CallService {
    private static final Logger logger = LoggerFactory.getLogger(CallService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    
    // Maps chatId to its active call
    private final Map<Long, ActiveCall> activeCalls = new ConcurrentHashMap<>();
    
    @Autowired
    public CallService(SimpMessagingTemplate messagingTemplate,
                      ChatRepository chatRepository,
                      UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Starts a new call
     * @param chatId the chat ID
     * @param initiatorId the user starting the call
     * @param callType audio or video
     * @return the created call event DTO
     */
    public CallEventDTO startCall(Long chatId, Long initiatorId, CallType callType) {
        logger.info("Starting {} call in chat {} by user {}", callType, chatId, initiatorId);
        
        // Get user and chat info
        User initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Chat chat = chatRepository.findByIdWithParticipants(chatId);
        if (chat == null) {
            throw new IllegalArgumentException("Chat not found");
        }
        
        boolean isGroupChat = chat.isGroupChat();
        
        // Check if there's already an active call in this chat
        if (activeCalls.containsKey(chatId)) {
            ActiveCall existingCall = activeCalls.get(chatId);
            if (existingCall.hasAnyActiveParticipant()) {
                logger.info("Call already in progress in chat {}", chatId);
                
                // Add user to existing call if not already a participant
                if (!existingCall.isActive(initiatorId)) {
                    existingCall.addParticipant(initiatorId);
                }
                
                // Convert to DTO for response
                CallEventDTO callEvent = new CallEventDTO(
                    CallEventType.CALL_NOTIFICATION,
                    chatId,
                    existingCall.getId(),
                    existingCall.getInitiatorId(),
                    existingCall.getInitiatorName(),
                    existingCall.getCallType()
                );
                
                // Add all active participants
                existingCall.getParticipants().forEach(callEvent::addParticipant);
                
                return callEvent;
            } else {
                // There was a call but it has ended, replace it
                logger.info("Replacing ended call in chat {}", chatId);
                activeCalls.remove(chatId);
            }
        }
        
        // Create a new call
        ActiveCall newCall = new ActiveCall(
            chatId,
            initiatorId,
            initiator.getName(),
            callType,
            isGroupChat
        );
        
        // Store the call
        activeCalls.put(chatId, newCall);
        
        // Create the call event
        CallEventDTO callEvent = new CallEventDTO(
            isGroupChat ? CallEventType.CALL_NOTIFICATION : CallEventType.OFFER,
            chatId,
            newCall.getId(),
            initiatorId,
            initiator.getName(),
            callType
        );
        
        // Add initiator as participant
        callEvent.addParticipant(initiatorId, true);
        
        logger.info("Created new call: id={}, chatId={}, isGroupCall={}", 
                    newCall.getId(), chatId, isGroupChat);
        
        return callEvent;
    }
    
    /**
     * Handles a call offer from a user to another user in a direct chat
     */
    public void processCallOffer(CallEventDTO callEvent, String sdpOffer) {
        Long chatId = callEvent.getChatId();
        Long callId = callEvent.getCallId();
        Long senderId = callEvent.getSenderId();
        
        logger.info("Processing call offer for call {} in chat {} from user {}", 
                   callId, chatId, senderId);
        
        // Get the chat to find the recipient
        Chat chat = chatRepository.findByIdWithParticipants(chatId);
        if (chat == null || chat.isGroupChat()) {
            logger.error("Direct chat not found or is a group chat: {}", chatId);
            return;
        }
        
        // In a direct chat, find the other user
        Optional<User> recipient = chat.getParticipants().stream()
            .filter(user -> !user.getId().equals(senderId))
            .findFirst();
        
        if (recipient.isPresent()) {
            User recipientUser = recipient.get();
            callEvent.addToPayload("sdp", sdpOffer);
            
            // Send offer to recipient
            logger.info("Sending call offer to user {}", recipientUser.getId());
            messagingTemplate.convertAndSendToUser(
                recipientUser.getName(),  // Use username for user-specific messages
                "/queue/private",
                callEvent
            );
        } else {
            logger.error("Recipient not found in chat {}", chatId);
        }
    }
    
    /**
     * Handles a call answer from a user
     */
    public void processCallAnswer(CallEventDTO callEvent, String sdpAnswer) {
        Long chatId = callEvent.getChatId();
        Long callId = callEvent.getCallId();
        Long responderId = callEvent.getSenderId();
        
        logger.info("Processing call answer for call {} in chat {} from user {}", 
                   callId, chatId, responderId);
        
        // Get the active call
        ActiveCall activeCall = activeCalls.get(chatId);
        if (activeCall == null || !activeCall.getId().equals(callId)) {
            logger.error("Active call not found: chat={}, call={}", chatId, callId);
            return;
        }
        
        // Add/update participant
        activeCall.addParticipant(responderId);
        
        // Add SDP answer to payload
        callEvent.addToPayload("sdp", sdpAnswer);
        
        // In a direct chat, send answer to the initiator
        if (!activeCall.isGroupCall()) {
            Long initiatorId = activeCall.getInitiatorId();
            User initiator = userRepository.findById(initiatorId).orElse(null);
            
            if (initiator != null) {
                logger.info("Sending call answer to initiator {}", initiatorId);
                messagingTemplate.convertAndSendToUser(
                    initiator.getName(),
                    "/queue/private",
                    callEvent
                );
            }
        } else {
            // For group calls, broadcast to all participants
            broadcastToCallParticipants(activeCall, callEvent);
        }
    }
    
    /**
     * Processes ICE candidates for WebRTC
     */
    public void processIceCandidate(CallEventDTO callEvent, Object iceCandidate) {
        Long chatId = callEvent.getChatId();
        Long callId = callEvent.getCallId();
        Long senderId = callEvent.getSenderId();
        
        logger.info("Processing ICE candidate for call {} in chat {} from user {}", 
                   callId, chatId, senderId);
        
        // Get the active call
        ActiveCall activeCall = activeCalls.get(chatId);
        if (activeCall == null || !activeCall.getId().equals(callId)) {
            logger.error("Active call not found: chat={}, call={}", chatId, callId);
            return;
        }
        
        callEvent.addToPayload("candidate", iceCandidate);
        
        if (!activeCall.isGroupCall()) {
            // For direct calls, send to the other participant
            Chat chat = chatRepository.findByIdWithParticipants(chatId);
            if (chat != null) {
                // Find other participant
                Optional<User> otherUser = chat.getParticipants().stream()
                    .filter(user -> !user.getId().equals(senderId))
                    .findFirst();
                
                if (otherUser.isPresent()) {
                    logger.info("Forwarding ICE candidate to user {}", otherUser.get().getId());
                    messagingTemplate.convertAndSendToUser(
                        otherUser.get().getName(),
                        "/queue/private",
                        callEvent
                    );
                }
            }
        } else {
            // For group calls, send to all other participants
            broadcastToCallParticipantsExcept(activeCall, callEvent, senderId);
        }
    }
    
    /**
     * Ends a call
     */
    public void endCall(Long chatId, Long callId, Long userId) {
        logger.info("Ending call {} in chat {} by user {}", callId, chatId, userId);
        
        // Get the active call
        ActiveCall activeCall = activeCalls.get(chatId);
        if (activeCall == null || !activeCall.getId().equals(callId)) {
            logger.error("Active call not found: chat={}, call={}", chatId, callId);
            return;
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.error("User not found: {}", userId);
            return;
        }
        
        if (!activeCall.isGroupCall()) {
            // For direct calls, ending by any participant ends the call
            activeCall.endCall();
            
            // Send end event to both participants
            Chat chat = chatRepository.findByIdWithParticipants(chatId);
            if (chat != null) {
                CallEventDTO endEvent = CallEventDTO.createCallEnded(
                    chatId, callId, userId, user.getName(), activeCall.getCallType()
                );
                
                for (User participant : chat.getParticipants()) {
                    if (!participant.getId().equals(userId)) {
                        messagingTemplate.convertAndSendToUser(
                            participant.getName(),
                            "/queue/private",
                            endEvent
                        );
                    }
                }
            }
            
            // Remove the call after a delay (could be handled by a scheduled task)
            // For simplicity, we'll just remove it immediately
            activeCalls.remove(chatId);
            
        } else {
            // For group calls, just remove the participant
            activeCall.removeParticipant(userId);
            
            // If no participants left, end the call
            if (!activeCall.hasAnyActiveParticipant()) {
                activeCall.endCall();
                activeCalls.remove(chatId);
            } else {
                // Otherwise broadcast participant left
                CallEventDTO updateEvent = new CallEventDTO(
                    CallEventType.CALL_NOTIFICATION,
                    chatId,
                    callId,
                    activeCall.getInitiatorId(),
                    activeCall.getInitiatorName(),
                    activeCall.getCallType()
                );
                
                // Add current participants
                activeCall.getParticipants().forEach(updateEvent::addParticipant);
                
                broadcastToCallParticipants(activeCall, updateEvent);
            }
        }
    }
    
    /**
     * Invites a user to join a group call
     */
    public void inviteToCall(Long chatId, Long callId, Long inviterId, Long inviteeId) {
        logger.info("User {} inviting user {} to call {} in chat {}", 
                   inviterId, inviteeId, callId, chatId);
        
        // Get the active call
        ActiveCall activeCall = activeCalls.get(chatId);
        if (activeCall == null || !activeCall.getId().equals(callId) || !activeCall.isGroupCall()) {
            logger.error("Active group call not found: chat={}, call={}", chatId, callId);
            return;
        }
        
        User inviter = userRepository.findById(inviterId).orElse(null);
        User invitee = userRepository.findById(inviteeId).orElse(null);
        
        if (inviter == null || invitee == null) {
            logger.error("Inviter or invitee not found");
            return;
        }
        
        // Create invite event
        CallEventDTO inviteEvent = CallEventDTO.createCallInvite(
            chatId,
            callId,
            inviterId,
            inviter.getName(),
            activeCall.getCallType(),
            inviteeId
        );
        
        // Add current participants to the invite event
        activeCall.getParticipants().forEach(inviteEvent::addParticipant);
        
        // Send invite to the invitee
        messagingTemplate.convertAndSendToUser(
            invitee.getName(),
            "/queue/private",
            inviteEvent
        );
    }
    
    /**
     * Broadcasts a call event to all active participants
     */
    private void broadcastToCallParticipants(ActiveCall call, CallEventDTO event) {
        // Get participants who are active in the call
        Set<Long> activeParticipantIds = call.getParticipants().entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        // Get participant user records
        Set<User> activeParticipants = userRepository.findAllById(activeParticipantIds)
            .stream()
            .collect(Collectors.toSet());
        
        // Send event to each participant
        for (User participant : activeParticipants) {
            messagingTemplate.convertAndSendToUser(
                participant.getName(),
                "/queue/private",
                event
            );
        }
    }
    
    /**
     * Broadcasts a call event to all active participants except one
     */
    private void broadcastToCallParticipantsExcept(ActiveCall call, CallEventDTO event, Long excludeUserId) {
        // Get participants who are active in the call, excluding the specified one
        Set<Long> activeParticipantIds = call.getParticipants().entrySet().stream()
            .filter(entry -> entry.getValue() && !entry.getKey().equals(excludeUserId))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        // Get participant user records
        Set<User> activeParticipants = userRepository.findAllById(activeParticipantIds)
            .stream()
            .collect(Collectors.toSet());
        
        // Send event to each participant
        for (User participant : activeParticipants) {
            messagingTemplate.convertAndSendToUser(
                participant.getName(),
                "/queue/private",
                event
            );
        }
    }
    
    /**
     * Gets information about active calls
     */
    public Map<Long, ActiveCall> getActiveCalls() {
        return new HashMap<>(activeCalls);
    }
    
    /**
     * Gets a specific active call
     */
    public ActiveCall getActiveCall(Long chatId) {
        return activeCalls.get(chatId);
    }
} 