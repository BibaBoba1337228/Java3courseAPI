package course.project.API.repositories;

import course.project.API.models.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByChatId(Long chatId, Pageable pageable);
    
    @EntityGraph(attributePaths = {"attachments"})
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt DESC")
    Page<Message> findByChatIdWithAttachmentsAndSender(@Param("chatId") Long chatId, Pageable pageable);
    
    @Query("SELECT m.id, u FROM Message m JOIN m.readBy u WHERE m.id IN :messageIds")
    List<Object[]> loadReadByForMessages(@Param("messageIds") List<Long> messageIds);

    @Query(value = """
            SELECT a.file_path, a.original_file_name, a.file_type from messages m
            inner join message_attachments a on a.message_id = m.id
            where m.chat_id = :chatId and m.id = :messageId and a.id = :attachmentId
            """,
    nativeQuery = true)
    Object findMessageAttachementFilePathByChatIdAndMessageIdAndAttachementId(@Param("chatId") Long chatId, @Param("messageId") Long messageId, @Param("attachmentId") Long attachmentId);
} 