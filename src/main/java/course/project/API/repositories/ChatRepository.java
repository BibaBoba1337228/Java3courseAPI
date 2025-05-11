package course.project.API.repositories;

import course.project.API.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByParticipantsId(Long userId);
    boolean existsByIdAndParticipantsId(Long chatId, Long userId);
} 