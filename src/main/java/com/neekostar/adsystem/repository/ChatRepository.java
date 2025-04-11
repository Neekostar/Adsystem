package com.neekostar.adsystem.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.neekostar.adsystem.model.Chat;
import com.neekostar.adsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {
    List<Chat> findByUser1OrUser2(User user1, User user2);

    Optional<Chat> findByUser1AndUser2(User user1, User user2);

    Optional<Chat> findByUser2AndUser1(User user2, User user1);
}
