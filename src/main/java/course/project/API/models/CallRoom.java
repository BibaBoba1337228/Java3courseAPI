package course.project.API.models;

import course.project.API.dto.call.CallType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CallRoom {
    private final String roomId;
    private final Long chatId;
    private final Long creatorId;
    private final String creatorName;
    private final CallType callType;
    private final Map<Long, Boolean> participants;
    private final boolean isGroupCall;

    public CallRoom(Long chatId, Long creatorId, String creatorName, CallType callType, boolean isGroupCall) {
        this.roomId = "room_" + System.currentTimeMillis();
        this.chatId = chatId;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.callType = callType;
        this.isGroupCall = isGroupCall;
        this.participants = new ConcurrentHashMap<>();
        this.participants.put(creatorId, true);
    }

    public String getRoomId() {
        return roomId;
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public CallType getCallType() {
        return callType;
    }

    public Map<Long, Boolean> getParticipants() {
        return participants;
    }

    public boolean isGroupCall() {
        return isGroupCall;
    }

    public void addParticipant(Long userId) {
        participants.put(userId, true);
    }

    public void removeParticipant(Long userId) {
        participants.remove(userId);
    }

    public boolean hasParticipant(Long userId) {
        return participants.containsKey(userId);
    }

    public boolean isActive(Long userId) {
        return participants.getOrDefault(userId, false);
    }

    public int getActiveParticipantsCount() {
        return (int) participants.values().stream().filter(Boolean::booleanValue).count();
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }
} 