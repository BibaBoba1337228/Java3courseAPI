package course.project.API.controllers;

import course.project.API.dto.call.CallEventDTO;
import course.project.API.dto.call.CallEventType;
import course.project.API.dto.call.CallType;
import course.project.API.models.ActiveCall;
import course.project.API.models.CallRoom;
import course.project.API.models.Chat;
import course.project.API.models.User;
import course.project.API.repositories.ChatRepository;
import course.project.API.services.CallService;
import course.project.API.services.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class CallWebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(CallWebSocketController.class);
    
    private final CallService callService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepository chatRepository;
    private final WebSocketService webSocketService;
    
    @Autowired
    public CallWebSocketController(CallService callService, 
                                  SimpMessagingTemplate messagingTemplate, 
                                  ChatRepository chatRepository,
                                  WebSocketService webSocketService) {
        this.callService = callService;
        this.messagingTemplate = messagingTemplate;
        this.chatRepository = chatRepository;
        this.webSocketService = webSocketService;
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
            
            // Создаем звонок без SDP offer - первая фаза сигнализации
            CallEventDTO callEvent = callService.startCall(chatId, user.getId(), callType);
            
            // Проверяем тип события, которое вернул сервис
            if (callEvent.getType() == CallEventType.CALL_BUSY) {
                // Если пользователь занят, отправляем только инициатору
                logger.info("User is busy, sending CALL_BUSY to initiator: {}", user.getName());
                webSocketService.sendPrivateMessageToUser(
                    user.getUsername(),
                    callEvent
                );
                return;
            }
            
            // Отправляем уведомление инициатору с callId через его приватную очередь
            // Это гарантирует, что инициатор получит callId до того, как начнет отправлять ICE кандидаты
            CallEventDTO initiatorNotification = new CallEventDTO();
            initiatorNotification.setType(CallEventType.CALL_NOTIFICATION);
            initiatorNotification.setChatId(chatId);
            initiatorNotification.setCallId(callEvent.getCallId());
            initiatorNotification.setSenderId(user.getId());
            initiatorNotification.setSenderName(user.getName());
            initiatorNotification.setCallType(callType);
            initiatorNotification.addParticipant(user.getId(), true);
            
            logger.info("Sending call notification to initiator: {}", user.getName());
            webSocketService.sendPrivateMessageToUser(
                user.getUsername(),
                initiatorNotification
            );
            
            // Для прямых и групповых чатов, отправляем уведомление всем участникам через приватные очереди
            Chat chat = chatRepository.findByIdWithParticipants(chatId);
            if (chat != null) {
                // Для групповых чатов отправляем уведомление всем участникам
                if (chat.isGroupChat()) {
                    // Создаем уведомление о начале звонка для всех участников
                    CallEventDTO callStartNotification = new CallEventDTO();
                    callStartNotification.setType(CallEventType.CALL_NOTIFICATION);
                    callStartNotification.setChatId(chatId);
                    callStartNotification.setCallId(callEvent.getCallId());
                    callStartNotification.setSenderId(user.getId());
                    callStartNotification.setSenderName(user.getName());
                    callStartNotification.setCallType(callType);
                    callStartNotification.addParticipant(user.getId(), true);

                    // Отправляем уведомление всем участникам группового чата
                    for (User participant : chat.getParticipants()) {
                        if (!participant.getId().equals(user.getId())) {
                            logger.info("Sending call notification to group chat member: {}", participant.getName());
                            webSocketService.sendPrivateMessageToUser(
                                participant.getUsername(),
                                callStartNotification
                            );
                        }
                    }
                } else {
                    // Для прямых чатов отправляем уведомление только собеседнику
                    for (User participant : chat.getParticipants()) {
                        if (!participant.getId().equals(user.getId())) {
                            logger.info("Sending call notification to direct chat participant: {}", participant.getName());
                            webSocketService.sendPrivateMessageToUser(
                                participant.getUsername(),
                                callEvent
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error processing call start: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles call acceptance before SDP exchange
     */
    @MessageMapping("/call/accept")
    public void acceptCall(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received call acceptance: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long callId = Long.valueOf(payload.get("callId").toString());
            
            // Create the call accept event
            CallEventDTO callEvent = new CallEventDTO();
            callEvent.setType(CallEventType.CALL_ACCEPTED);
            callEvent.setChatId(chatId);
            callEvent.setCallId(callId);
            callEvent.setSenderId(user.getId());
            callEvent.setSenderName(user.getName());
            
            // Mark this user as having accepted the call
            callService.processCallAcceptance(callEvent);
            
        } catch (Exception e) {
            logger.error("Error processing call acceptance: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles WebRTC offer after call acceptance
     */
    @MessageMapping("/call/offer")
    public void sendOffer(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received call offer: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long callId = Long.valueOf(payload.get("callId").toString());
            
            // Поддержка разных форматов SDP offer
            String sdpOffer = null;
            if (payload.containsKey("sdp")) {
                sdpOffer = payload.get("sdp").toString();
                logger.info("Found SDP in 'sdp' field");
            } else if (payload.containsKey("offer") && payload.get("offer") instanceof Map) {
                Map<String, Object> offerObj = (Map<String, Object>) payload.get("offer");
                if (offerObj.containsKey("sdp")) {
                    sdpOffer = offerObj.get("sdp").toString();
                    logger.info("Found SDP in 'offer.sdp' field");
                }
            }
            
            if (sdpOffer == null) {
                logger.error("No SDP offer found in payload");
                return;
            }
            
            // Create the call offer event
            CallEventDTO callEvent = new CallEventDTO();
            callEvent.setType(CallEventType.OFFER);
            callEvent.setChatId(chatId);
            callEvent.setCallId(callId);
            callEvent.setSenderId(user.getId());
            callEvent.setSenderName(user.getName());
            
            // Process the offer through the call service
            callService.processCallOffer(callEvent, sdpOffer);
            
        } catch (Exception e) {
            logger.error("Error processing call offer: {}", e.getMessage(), e);
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
            
            // Поддержка двух форматов: sdp напрямую или внутри объекта answer
            String sdpAnswer;
            if (payload.containsKey("sdp")) {
                sdpAnswer = payload.get("sdp").toString();
            } else if (payload.containsKey("answer") && payload.get("answer") instanceof Map) {
                Map<String, Object> answerObj = (Map<String, Object>) payload.get("answer");
                if (answerObj.containsKey("sdp")) {
                    sdpAnswer = answerObj.get("sdp").toString();
                } else {
                    logger.error("Invalid answer format - no SDP found in answer object");
                    return;
                }
            } else {
                logger.error("Invalid payload format - no sdp or answer object found");
                return;
            }
            
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
            
            // Поддержка разных форматов ICE кандидатов
            Object iceCandidate;
            if (payload.containsKey("candidate")) {
                iceCandidate = payload.get("candidate");
                logger.info("Found ICE candidate in root object: {}", iceCandidate);
            } else {
                logger.error("Invalid payload format - no candidate found in message");
                return;
            }
            
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
            
            if (payload == null) {
                logger.error("Received null payload for call end");
                return;
            }
            
            Object chatIdObj = payload.get("chatId");
            Object callIdObj = payload.get("callId");
            
            if (chatIdObj == null || callIdObj == null) {
                logger.error("Missing required fields in call end payload: chatId={}, callId={}", 
                           chatIdObj, callIdObj);
                return;
            }
            
            Long chatId = Long.valueOf(chatIdObj.toString());
            Long callId = Long.valueOf(callIdObj.toString());
            
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
            User initiator = webSocketService.getUserById(Long.valueOf(payload.get("initiatorId").toString()));
            if (initiator != null) {
                webSocketService.sendPrivateMessageToUser(
                    initiator.getUsername(),
                    callEvent
                );
            }
            
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
                    // Для групповых звонков, используем broadcastToCallParticipantsExcept из CallService
                    callService.broadcastMediaStatus(activeCall, callEvent, user.getId());
                } else {
                    // For direct chats, send to the other participant
                    Chat chat = chatRepository.findByIdWithParticipants(chatId);
                    if (chat != null) {
                        for (User participant : chat.getParticipants()) {
                            if (!participant.getId().equals(user.getId())) {
                                webSocketService.sendPrivateMessageToUser(
                                    participant.getUsername(),
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
    
    /**
     * Создает новую комнату для звонка
     */
    @MessageMapping("/call/room/create")
    public void createRoom(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received room creation request: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            String callTypeStr = payload.get("callType").toString();
            CallType callType = CallType.valueOf(callTypeStr.toUpperCase());
            
            // Создаем комнату
            CallEventDTO roomEvent = callService.createCallRoom(chatId, user.getId(), callType);
            
            // Отправляем уведомление о создании комнаты всем участникам чата
            Chat chat = chatRepository.findByIdWithParticipants(chatId);
            if (chat != null) {
                for (User participant : chat.getParticipants()) {
                    webSocketService.sendPrivateMessageToUser(
                        participant.getUsername(),
                        roomEvent
                    );
                }
            }
        } catch (Exception e) {
            logger.error("Error creating call room: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Приглашает пользователя в комнату
     */
    @MessageMapping("/call/room/invite")
    public void inviteToRoom(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received room invite request: {}", payload);
            
            String roomId = payload.get("roomId").toString();
            Long inviteeId = Long.valueOf(payload.get("inviteeId").toString());
            
            // Отправляем приглашение
            callService.inviteToRoom(roomId, user.getId(), inviteeId);
            
        } catch (Exception e) {
            logger.error("Error inviting to room: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Присоединяется к комнате
     */
    @MessageMapping("/call/room/join")
    public void joinRoom(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received room join request: {}", payload);
            
            String roomId = payload.get("roomId").toString();
            
            // Присоединяемся к комнате
            callService.joinRoom(roomId, user.getId());
            
        } catch (Exception e) {
            logger.error("Error joining room: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Покидает комнату
     */
    @MessageMapping("/call/room/leave")
    public void leaveRoom(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received room leave request: {}", payload);
            
            String roomId = payload.get("roomId").toString();
            
            // Покидаем комнату
            callService.leaveRoom(roomId, user.getId());
            
        } catch (Exception e) {
            logger.error("Error leaving room: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Получает информацию о комнате для звонка в чате
     */
    @MessageMapping("/call/room/info")
    public void getRoomInfo(@Payload Map<String, Object> payload, @AuthenticationPrincipal User user) {
        try {
            logger.info("Received room info request: {}", payload);
            
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            
            // Получаем информацию о комнате
            CallRoom room = callService.getRoomByChat(chatId);
            
            // Создаем ответ
            CallEventDTO roomInfoEvent;
            
            if (room != null) {
                // Комната существует
                roomInfoEvent = new CallEventDTO(
                    CallEventType.ROOM_INFO,
                    chatId,
                    null,
                    room.getCreatorId(),
                    room.getCreatorName(),
                    room.getCallType()
                );
                
                roomInfoEvent.addToPayload("roomId", room.getRoomId());
                roomInfoEvent.addToPayload("exists", true);
                roomInfoEvent.addToPayload("isGroupCall", room.isGroupCall());
                room.getParticipants().forEach(roomInfoEvent::addParticipant);
            } else {
                // Комнаты нет
                roomInfoEvent = new CallEventDTO(
                    CallEventType.ROOM_INFO,
                    chatId,
                    null,
                    user.getId(),
                    user.getName(),
                    null
                );
                
                roomInfoEvent.addToPayload("exists", false);
            }
            
            // Отправляем ответ пользователю
            webSocketService.sendPrivateMessageToUser(
                user.getUsername(),
                roomInfoEvent
            );
        } catch (Exception e) {
            logger.error("Error getting room info: {}", e.getMessage(), e);
        }
    }
} 