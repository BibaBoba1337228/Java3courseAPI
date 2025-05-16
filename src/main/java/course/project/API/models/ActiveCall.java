package course.project.API.models;

import course.project.API.dto.call.CallType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active call session (not persisted to database)
 */
public class ActiveCall {
    private Long id;
    private Long chatId;
    private Long initiatorId;
    private String initiatorName;
    private CallType callType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isGroupCall;
    private Map<Long, Boolean> participants; // userId -> active status

    public ActiveCall() {
        this.id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; // Generate positive random ID
        this.startTime = LocalDateTime.now();
        this.participants = new ConcurrentHashMap<>();
    }

    public ActiveCall(Long chatId, Long initiatorId, String initiatorName, CallType callType, boolean isGroupCall) {
        this();
        this.chatId = chatId;
        this.initiatorId = initiatorId;
        this.initiatorName = initiatorName;
        this.callType = callType;
        this.isGroupCall = isGroupCall;
        // Add initiator as first participant
        this.participants.put(initiatorId, true);
    }

    public void addParticipant(Long userId) {
        this.participants.put(userId, true);
    }

    public void removeParticipant(Long userId) {
        this.participants.put(userId, false);
    }

    public boolean isActive(Long userId) {
        return this.participants.getOrDefault(userId, false);
    }

    public boolean hasAnyActiveParticipant() {
        return this.participants.values().stream().anyMatch(active -> active);
    }

    public int getActiveParticipantCount() {
        return (int) this.participants.values().stream().filter(active -> active).count();
    }

    public void endCall() {
        this.endTime = LocalDateTime.now();
        // Mark all participants as inactive
        for (Long userId : this.participants.keySet()) {
            this.participants.put(userId, false);
        }
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(Long initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public void setInitiatorName(String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isGroupCall() {
        return isGroupCall;
    }

    public void setGroupCall(boolean groupCall) {
        isGroupCall = groupCall;
    }

    public Map<Long, Boolean> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<Long, Boolean> participants) {
        this.participants = participants;
    }
} 