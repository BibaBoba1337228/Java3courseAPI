package course.project.API.dto.call;

public enum CallEventType {
    // Signaling events
    OFFER,              // Initial call request with SDP offer
    ANSWER,             // Call answer with SDP answer
    ICE_CANDIDATE,      // WebRTC ICE candidate
    
    // Call status events
    CALL_STARTED,       // Call has been established
    CALL_ENDED,         // Call has ended
    CALL_REJECTED,      // Call was rejected
    CALL_MISSED,        // Call was not answered
    CALL_ACCEPTED,      // Call was accepted, ready to exchange SDP offer/answer
    CALL_BUSY,          // User is already in another call
    
    // Room events
    ROOM_CREATED,       // New call room has been created
    ROOM_INVITE,        // User has been invited to join a call room
    ROOM_JOINED,        // User has joined the call room
    ROOM_LEFT,          // User has left the call room
    ROOM_INFO,          // Information about existing room for a chat
    
    // Notifications
    CALL_NOTIFICATION,  // For group chats - notification about active call
    CALL_INVITE,        // For group chats - specific invitation to join a call
    
    // Media control
    TOGGLE_AUDIO,       // Mute/unmute audio
    TOGGLE_VIDEO,       // Enable/disable video
    SCREEN_SHARE_STARTED,
    SCREEN_SHARE_ENDED
} 