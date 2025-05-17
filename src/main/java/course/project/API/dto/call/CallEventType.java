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
    
    // Notifications
    CALL_NOTIFICATION,  // For group chats - notification about active call
    CALL_INVITE,        // For group chats - specific invitation to join a call
    
    // Media control
    TOGGLE_AUDIO,       // Mute/unmute audio
    TOGGLE_VIDEO,       // Enable/disable video
    SCREEN_SHARE_STARTED,
    SCREEN_SHARE_ENDED
} 