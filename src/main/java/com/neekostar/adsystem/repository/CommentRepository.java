package com.neekostar.adsystem.repository;

import java.util.UUID;
import com.neekostar.adsystem.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    void deleteByAdId(UUID adId);
}
