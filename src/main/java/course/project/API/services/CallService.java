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
    private final WebSocketService webSocketService;
    
    // Maps callId to ActiveCall - changed from chatId to callId
    private final Map<Long, ActiveCall> activeCalls = new ConcurrentHashMap<>();
    
    // Secondary index to quickly find calls by chatId
    private final Map<Long, Long> chatToCallMap = new ConcurrentHashMap<>();
    
    @Autowired
    public CallService(SimpMessagingTemplate messagingTemplate,
                      ChatRepository chatRepository,
                      UserRepository userRepository,
                      WebSocketService webSocketService) {
        this.messagingTemplate = messagingTemplate;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
    }
    
    /**
     * Starts a new call
     * @param chatId the chat ID
     * @param initiatorId the user starting the call
     * @param callType audio or video
     * @return the created call event DTO
     */
    public CallEventDTO startCall(Long chatId, Long initiatorId, CallType callType) {
        logger.info("[CALL] Starting {} call in chat {} by user {}", callType, chatId, initiatorId);
        
        // Get user and chat info
        User initiator = userRepository.findById(initiatorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Chat chat = chatRepository.findByIdWithParticipants(chatId);
        if (chat == null) {
            throw new IllegalArgumentException("Chat not found");
        }
        
        boolean isGroupChat = chat.isGroupChat();
        
        // Check if there's already an active call in this chat
        Long existingCallId = chatToCallMap.get(chatId);
        if (existingCallId != null) {
            ActiveCall existingCall = activeCalls.get(existingCallId);
            
            if (existingCall != null && existingCall.hasAnyActiveParticipant()) {
                logger.info("[CALL {}] Call already in progress in chat {}", existingCallId, chatId);
                
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
            } else if (existingCall != null) {
                // There was a call but it has ended, replace it
                logger.info("[CALL {}] Replacing ended call in chat {}", existingCallId, chatId);
                activeCalls.remove(existingCallId);
                chatToCallMap.remove(chatId);
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
        Long callId = newCall.getId();
        activeCalls.put(callId, newCall);
        chatToCallMap.put(chatId, callId);
        
        // Create the call event
        CallEventDTO callEvent = new CallEventDTO(
            isGroupChat ? CallEventType.CALL_NOTIFICATION : CallEventType.OFFER,
            chatId,
            callId,
            initiatorId,
            initiator.getName(),
            callType
        );
        
        // Add initiator as participant
        callEvent.addParticipant(initiatorId, true);
        
        logger.info("[CALL {}] Created new call: chatId={}, isGroupCall={}", 
                    callId, chatId, isGroupChat);
        
        // Отправить уведомление инициатору через приватную очередь, чтобы он гарантированно 
        // получил callId до того, как начнет отправлять ICE кандидаты
        CallEventDTO initiatorNotification = new CallEventDTO(
            CallEventType.CALL_NOTIFICATION,
            chatId,
            callId,
            initiatorId,
            initiator.getName(),
            callType
        );
        initiatorNotification.addParticipant(initiatorId, true);
        
        logger.info("[CALL {}] Sending call notification to initiator {}", callId, initiatorId);
        
        // Используем WebSocketService вместо messagingTemplate
        webSocketService.sendPrivateMessageToUser(
            initiator.getUsername(),
            initiatorNotification
        );
        
        return callEvent;
    }
    
    /**
     * Handles a call offer from a user to another user in a direct chat
     */
    public void processCallOffer(CallEventDTO callEvent, String sdpOffer) {
        Long chatId = callEvent.getChatId();
        Long callId = callEvent.getCallId();
        Long senderId = callEvent.getSenderId();
        
        logger.info("[CALL {}] Processing call offer in chat {} from user {}", 
                   callId, chatId, senderId);
        
        // Save SDP in call notification event for both direct and group chats
        callEvent.addToPayload("sdp", sdpOffer);
        
        // Get the active call and make sure the sender is added as a participant
        ActiveCall activeCall = findActiveCall(callId, chatId);
        if (activeCall == null) {
            logger.error("[CALL {}] Active call not found for call offer", callId);
            return;
        }
        
        // Make sure sender is added as a participant (should be already done when creating the call)
        if (!activeCall.getParticipants().containsKey(senderId)) {
            logger.info("[CALL {}] Adding initiator {} as participant", callId, senderId);
            activeCall.addParticipant(senderId);
        }
        
        // Get the chat to find the recipient
        Chat chat = chatRepository.findByIdWithParticipants(chatId);
        if (chat == null) {
            logger.error("[CALL {}] Chat not found: {}", callId, chatId);
            return;
        }
        
        if (!chat.isGroupChat()) {
            // In a direct chat, find the other user
            Optional<User> recipient = chat.getParticipants().stream()
                .filter(user -> !user.getId().equals(senderId))
                .findFirst();
            
            if (recipient.isPresent()) {
                User recipientUser = recipient.get();
                
                // Add recipient to participants list (with inactive status initially)
                // This will help ensure ICE candidates can be forwarded even before they answer
                if (!activeCall.getParticipants().containsKey(recipientUser.getId())) {
                    logger.info("[CALL {}] Adding recipient {} as potential participant", callId, recipientUser.getId());
                    activeCall.getParticipants().put(recipientUser.getId(), false);
                }
                
                // Send offer to recipient
                logger.info("[CALL {}] Sending call offer to user {} ({})", callId, recipientUser.getId(), recipientUser.getName());
                webSocketService.sendPrivateMessageToUser(
                    recipientUser.getUsername(),  // Use username for user-specific messages
                    callEvent
                );
            } else {
                logger.error("[CALL {}] Recipient not found in chat {}", callId, chatId);
            }
        }
        // For group chats, the controller handles broadcasting
    }
    
    /**
     * Handles a call answer from a user
     */
    public void processCallAnswer(CallEventDTO callEvent, String sdpAnswer) {
        Long chatId = callEvent.getChatId();
        Long callId = callEvent.getCallId();
        Long responderId = callEvent.getSenderId();
        
        logger.info("[CALL {}] Processing call answer in chat {} from user {}", 
                   callId, chatId, responderId);
        
        // Get the active call
        ActiveCall activeCall = findActiveCall(callId, chatId);
        if (activeCall == null) {
            logger.error("[CALL {}] Active call not found. Sending CALL_ENDED to client", callId);
            sendCallEndedEvent(callId, chatId, responderId);
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
                logger.info("[CALL {}] Sending call answer to initiator {} (username: {})", 
                           callId, initiatorId, initiator.getName());
                
                try {
                    webSocketService.sendPrivateMessageToUser(
                        initiator.getUsername(),
                        callEvent
                    );
                    logger.info("[CALL {}] Successfully sent answer to initiator via WebSocketService", callId);
                } catch (Exception e) {
                    logger.error("[CALL {}] Error sending answer to initiator: {}", 
                                callId, e.getMessage(), e);
                }
            } else {
                logger.error("[CALL {}] Initiator user with ID {} not found", callId, initiatorId);
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
        
        logger.info("[CALL {}] Processing ICE candidate in chat {} from user {}, candidate: {}", 
                   callId, chatId, senderId, iceCandidate);
        
        // Get the active call
        ActiveCall activeCall = findActiveCall(callId, chatId);
        if (activeCall == null) {
            logger.error("[CALL {}] Active call not found for ICE candidate. Sending CALL_ENDED to client", callId);
            sendCallEndedEvent(callId, chatId, senderId);
            return;
        }
        
        logger.info("[CALL {}] Found active call. Group call: {}, Participants: {}", 
                   callId, activeCall.isGroupCall(), activeCall.getParticipants());
        
        callEvent.addToPayload("candidate", iceCandidate);
        
        if (!activeCall.isGroupCall()) {
            // For direct calls, send to the other participant
            Chat chat = chatRepository.findByIdWithParticipants(chatId);
            if (chat != null) {
                logger.info("[CALL {}] Direct call chat participants: {}", callId, 
                         chat.getParticipants().stream()
                            .map(u -> u.getId() + ":" + u.getName())
                            .collect(Collectors.joining(", ")));
                
                // Find other participant
                Optional<User> otherUser = chat.getParticipants().stream()
                    .filter(user -> !user.getId().equals(senderId))
                    .findFirst();
                
                if (otherUser.isPresent()) {
                    // Make sure other user is added as a participant
                    if (!activeCall.getParticipants().containsKey(otherUser.get().getId())) {
                        logger.info("[CALL {}] Adding missing participant {} to call", callId, otherUser.get().getId());
                        activeCall.addParticipant(otherUser.get().getId());
                    }
                    
                    logger.info("[CALL {}] Forwarding ICE candidate to user {} ({})", 
                              callId, otherUser.get().getId(), otherUser.get().getName());
                    
                    // Используем WebSocketService вместо messagingTemplate
                    webSocketService.sendPrivateMessageToUser(
                        otherUser.get().getUsername(),
                        callEvent
                    );
                } else {
                    logger.error("[CALL {}] Could not find other participant in chat {}", callId, chatId);
                }
            } else {
                logger.error("[CALL {}] Chat not found: {}", callId, chatId);
            }
        } else {
            // For group calls, send to all other participants
            logger.info("[CALL {}] Broadcasting ICE candidate to all participants except sender", callId);
            broadcastToCallParticipantsExcept(activeCall, callEvent, senderId);
        }
    }
    
    /**
     * Ends a call
     */
    public void endCall(Long chatId, Long callId, Long userId) {
        logger.info("[CALL {}] Ending call in chat {} by user {}", callId, chatId, userId);
        
        // Get the active call
        ActiveCall activeCall = findActiveCall(callId, chatId);
        if (activeCall == null) {
            logger.error("[CALL {}] Active call not found. Sending CALL_ENDED to client", callId);
            sendCallEndedEvent(callId, chatId, userId);
            return;
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.error("[CALL {}] User not found: {}", callId, userId);
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
                        webSocketService.sendPrivateMessageToUser(
                            participant.getUsername(),
                            endEvent
                        );
                    }
                }
            }
            
            // Remove the call
            activeCalls.remove(callId);
            chatToCallMap.remove(chatId);
            
        } else {
            // For group calls, just remove the participant
            activeCall.removeParticipant(userId);
            
            // If no participants left, end the call
            if (!activeCall.hasAnyActiveParticipant()) {
                activeCall.endCall();
                activeCalls.remove(callId);
                chatToCallMap.remove(chatId);
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
        logger.info("[CALL {}] User {} inviting user {} to call in chat {}", 
                   callId, inviterId, inviteeId, chatId);
        
        // Get the active call
        ActiveCall activeCall = findActiveCall(callId, chatId);
        if (activeCall == null) {
            logger.error("[CALL {}] Active call not found. Sending CALL_ENDED to client", callId);
            sendCallEndedEvent(callId, chatId, inviterId);
            return;
        }
        
        if (!activeCall.isGroupCall()) {
            logger.error("[CALL {}] Cannot invite to non-group call", callId);
            return;
        }
        
        User inviter = userRepository.findById(inviterId).orElse(null);
        User invitee = userRepository.findById(inviteeId).orElse(null);
        
        if (inviter == null || invitee == null) {
            logger.error("[CALL {}] Inviter or invitee not found", callId);
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
        webSocketService.sendPrivateMessageToUser(
            invitee.getUsername(),
            inviteEvent
        );
    }
    
    /**
     * Find an active call by callId and verify it belongs to the specified chatId
     */
    private ActiveCall findActiveCall(Long callId, Long chatId) {
        ActiveCall call = activeCalls.get(callId);
        
        // First try to find by callId (primary lookup)
        if (call != null && call.getChatId().equals(chatId)) {
            return call;
        }
        
        // If not found, try to find by chatId (legacy fallback)
        Long mappedCallId = chatToCallMap.get(chatId);
        if (mappedCallId != null) {
            call = activeCalls.get(mappedCallId);
            if (call != null) {
                return call;
            }
        }
        
        return null;
    }
    
    /**
     * Send a CALL_ENDED event to a user when call is not found
     */
    private void sendCallEndedEvent(Long callId, Long chatId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        
        CallEventDTO endEvent = new CallEventDTO();
        endEvent.setType(CallEventType.CALL_ENDED);
        endEvent.setChatId(chatId);
        endEvent.setCallId(callId);
        
        // Используем WebSocketService вместо messagingTemplate
        webSocketService.sendPrivateMessageToUser(
            user.getUsername(),
            endEvent
        );
    }
    
    /**
     * Broadcasts a call event to all active participants
     */
    private void broadcastToCallParticipants(ActiveCall call, CallEventDTO event) {
        logger.info("[CALL {}] Broadcasting to all participants, participants: {}", 
                  event.getCallId(), call.getParticipants());
        
        // Get participants who are active in the call
        Set<Long> activeParticipantIds = call.getParticipants().entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        logger.info("[CALL {}] Filtered active participants: {}", event.getCallId(), activeParticipantIds);
        
        // Get participant user records
        Set<User> activeParticipants = userRepository.findAllById(activeParticipantIds)
            .stream()
            .collect(Collectors.toSet());
        
        logger.info("[CALL {}] Found {} user records for active participants", 
                  event.getCallId(), activeParticipants.size());
        
        // Send event to each participant
        for (User participant : activeParticipants) {
            logger.info("[CALL {}] Sending event type {} to user {} ({})", 
                      event.getCallId(), event.getType(), participant.getId(), participant.getName());
            
            // Используем WebSocketService вместо messagingTemplate
            webSocketService.sendPrivateMessageToUser(
                participant.getUsername(),
                event
            );
        }
        
        if (activeParticipants.isEmpty()) {
            logger.warn("[CALL {}] No active participants to send event to", event.getCallId());
        }
    }
    
    /**
     * Broadcasts a call event to all active participants except one
     */
    private void broadcastToCallParticipantsExcept(ActiveCall call, CallEventDTO event, Long excludeUserId) {
        logger.info("[CALL {}] Broadcasting to participants except user {}, all participants: {}", 
                  event.getCallId(), excludeUserId, call.getParticipants());
        
        // Get participants who are active in the call, excluding the specified one
        Set<Long> activeParticipantIds = call.getParticipants().entrySet().stream()
            .filter(entry -> entry.getValue() && !entry.getKey().equals(excludeUserId))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        logger.info("[CALL {}] Filtered active participants: {}", event.getCallId(), activeParticipantIds);
        
        // Get participant user records
        Set<User> activeParticipants = userRepository.findAllById(activeParticipantIds)
            .stream()
            .collect(Collectors.toSet());
        
        logger.info("[CALL {}] Found {} user records for active participants", 
                  event.getCallId(), activeParticipants.size());
        
        // Send event to each participant
        for (User participant : activeParticipants) {
            logger.info("[CALL {}] Sending event type {} to user {} ({})", 
                      event.getCallId(), event.getType(), participant.getId(), participant.getName());
            
            // Используем WebSocketService вместо messagingTemplate
            webSocketService.sendPrivateMessageToUser(
                participant.getUsername(),
                event
            );
        }
        
        if (activeParticipants.isEmpty()) {
            logger.warn("[CALL {}] No active participants to send event to", event.getCallId());
        }
    }
    
    /**
     * Gets information about active calls
     */
    public Map<Long, ActiveCall> getActiveCalls() {
        return new HashMap<>(activeCalls);
    }
    
    /**
     * Gets a specific active call by chatId
     */
    public ActiveCall getActiveCall(Long chatId) {
        Long callId = chatToCallMap.get(chatId);
        if (callId != null) {
            return activeCalls.get(callId);
        }
        return null;
    }
    
    /**
     * Gets a specific active call by callId
     */
    public ActiveCall getActiveCallById(Long callId) {
        return activeCalls.get(callId);
    }
    
    /**
     * Broadcasts a media status event to all participants except the sender
     */
    public void broadcastMediaStatus(ActiveCall call, CallEventDTO event, Long excludeUserId) {
        logger.info("[CALL {}] Broadcasting media status to participants except user {}", 
                  event.getCallId(), excludeUserId);
        
        // Используем существующий метод для рассылки всем участникам, кроме отправителя
        broadcastToCallParticipantsExcept(call, event, excludeUserId);
    }
} 