package course.project.API.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;


    @Column(name = "is_group_chat", nullable = false)
    private boolean isGroupChat;

    @Column(name = "avatar_url")
    private String avatarURL;

    @ManyToMany
    @JoinTable(
        name = "chat_participants",
        joinColumns = @JoinColumn(name = "chat_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "chat_user_roles", joinColumns = @JoinColumn(name = "chat_id"))
    @MapKeyColumn(name = "user_id")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Map<Long, ChatRole> userRoles = new HashMap<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Message> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Chat() {
        this.createdAt = LocalDateTime.now();
    }

    public Chat(String name, boolean isGroupChat) {
        this();
        this.name = name;
        this.isGroupChat = isGroupChat;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public boolean isGroupChat() {
        return isGroupChat;
    }

    public void setGroupChat(boolean groupChat) {
        isGroupChat = groupChat;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public Set<User> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }

    public Map<Long, ChatRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Map<Long, ChatRole> userRoles) {
        this.userRoles = userRoles;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public void addParticipant(User user, ChatRole role) {
        participants.add(user);
        userRoles.put(user.getId(), role);
    }

    public void removeParticipant(User user) {
        participants.remove(user);
        userRoles.remove(user.getId());
    }

    public ChatRole getParticipantRole(User user) {
        return userRoles.get(user.getId());
    }

    public void setParticipantRole(User user, ChatRole role) {
        if (participants.contains(user)) {
            userRoles.put(user.getId(), role);
        }
    }

    public void addMessage(Message message) {
        messages.add(message);
        message.setChat(this);
        this.lastMessageAt = message.getCreatedAt();
    }

    public void removeMessage(Message message) {
        messages.remove(message);
        message.setChat(null);
    }
} 