package com.neekostar.adsystem.repository;

import java.util.List;
import java.util.UUID;
import com.neekostar.adsystem.model.Chat;
import com.neekostar.adsystem.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatOrderByCreatedAtAsc(Chat chat);

    List<Message> findByChatId(UUID chatId);
}
