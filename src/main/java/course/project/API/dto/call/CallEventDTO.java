package course.project.API.dto.call;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * DTO for WebRTC call signaling and events
 */
public class CallEventDTO {
    private CallEventType type;
    private Long chatId;
    private Long callId;
    private Long senderId;
    private String senderName;
    private CallType callType;
    private Map<String, Object> payload;
    private LocalDateTime timestamp;
    private Map<Long, Boolean> participants; // userId -> active status

    public CallEventDTO() {
        this.timestamp = LocalDateTime.now();
        this.payload = new HashMap<>();
        this.participants = new HashMap<>();
    }

    public CallEventDTO(CallEventType type, Long chatId, Long callId, Long senderId, String senderName, CallType callType) {
        this();
        this.type = type;
        this.chatId = chatId;
        this.callId = callId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.callType = callType;
    }

    // Factory methods for different event types
    public static CallEventDTO createOffer(Long chatId, Long callId, Long senderId, String senderName, 
                                          CallType callType, String sdpOffer) {
        CallEventDTO event = new CallEventDTO(CallEventType.OFFER, chatId, callId, senderId, senderName, callType);
        event.addToPayload("sdp", sdpOffer);
        return event;
    }

    public static CallEventDTO createAnswer(Long chatId, Long callId, Long senderId, String senderName, 
                                           CallType callType, String sdpAnswer) {
        CallEventDTO event = new CallEventDTO(CallEventType.ANSWER, chatId, callId, senderId, senderName, callType);
        event.addToPayload("sdp", sdpAnswer);
        return event;
    }

    public static CallEventDTO createIceCandidate(Long chatId, Long callId, Long senderId, String senderName, 
                                                 CallType callType, Object iceCandidate) {
        CallEventDTO event = new CallEventDTO(CallEventType.ICE_CANDIDATE, chatId, callId, senderId, senderName, callType);
        event.addToPayload("candidate", iceCandidate);
        return event;
    }

    public static CallEventDTO createCallNotification(Long chatId, Long callId, Long senderId, String senderName, 
                                                     CallType callType) {
        return new CallEventDTO(CallEventType.CALL_NOTIFICATION, chatId, callId, senderId, senderName, callType);
    }
    
    public static CallEventDTO createCallInvite(Long chatId, Long callId, Long senderId, String senderName, 
                                               CallType callType, Long invitedUserId) {
        CallEventDTO event = new CallEventDTO(CallEventType.CALL_INVITE, chatId, callId, senderId, senderName, callType);
        event.addToPayload("invitedUserId", invitedUserId);
        return event;
    }

    public static CallEventDTO createCallEnded(Long chatId, Long callId, Long senderId, String senderName, CallType callType) {
        return new CallEventDTO(CallEventType.CALL_ENDED, chatId, callId, senderId, senderName, callType);
    }

    public void addToPayload(String key, Object value) {
        this.payload.put(key, value);
    }

    public void addParticipant(Long userId, boolean active) {
        this.participants.put(userId, active);
    }

    // Getters and setters
    public CallEventType getType() {
        return type;
    }

    public void setType(CallEventType type) {
        this.type = type;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getCallId() {
        return callId;
    }

    public void setCallId(Long callId) {
        this.callId = callId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<Long, Boolean> getParticipants() {
        return participants;
    }
    
    public void setParticipants(Map<Long, Boolean> participants) {
        this.participants = participants;
    }
} 