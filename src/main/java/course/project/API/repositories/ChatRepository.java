package course.project.API.repositories;

import course.project.API.models.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByParticipantsId(Long userId);
    boolean existsByIdAndParticipantsId(Long chatId, Long userId);

    Page<Chat> findByParticipantsId(Long userId, Pageable pageable);

    @Query(value = """
        SELECT 
            c.id, c.name, c.is_group_chat, 
            m.id, m.content, m.created_at, m.is_edited,
            u.id, u.name, u.avatarurl, mrb.user_id
        FROM chats c
        JOIN chat_participants cp ON cp.chat_id = c.id
        LEFT JOIN LATERAL (
            SELECT * FROM messages m
            WHERE m.chat_id = c.id
            ORDER BY m.created_at DESC
            LIMIT 1
        ) m ON true
        LEFT JOIN users u ON m.sender_id = u.id
        LEFT JOIN message_read_by mrb ON mrb.message_id = m.id
        WHERE cp.user_id = :userId
        ORDER BY IF(m.created_at IS NULL, 1, 0), m.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
            SELECT c.id FROM chats c
            JOIN chat_participants cp ON cp.chat_id = c.id
            WHERE cp.user_id = :userId
        ) sub
        """,
        nativeQuery = true)
    Page<Object[]> findChatsWithLastMessageByUserId(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"participants"})
    @Query("SELECT c FROM Chat c WHERE c.id = :chatId")
    Chat findByIdWithParticipants(@Param("chatId") Long chatId);

    @EntityGraph(attributePaths = {"participants"})
    Optional<Chat> findProjectWithParticipantsOwnerById(Long id);




    @Query(value = """
        SELECT EXISTS(
            SELECT 1 FROM chats c
            JOIN chat_participants cp1 ON c.id = cp1.chat_id AND cp1.user_id = :userId1
            JOIN chat_participants cp2 ON c.id = cp2.chat_id AND cp2.user_id = :userId2
            WHERE c.is_group_chat = 0
        )
    """, nativeQuery = true)
    Long existsPersonalChatBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @EntityGraph(attributePaths = {"participants"})
    @Query("SELECT c from Chat c WHERE c.id IN :chatIds")
    List<Chat> findChatsByIdsWithParticipants(@Param("chatIds") List<Long> chatIds);
}