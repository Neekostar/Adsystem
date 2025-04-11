package com.neekostar.adsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "ads")
public class Ad {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "{ad.title.notblank}")
    @Size(min = 3, max = 255, message = "{ad.title.size}")
    @Column(nullable = false)
    private String title;

    @Size(max = 1000, message = "{ad.description.size}")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "{ad.price.notblank}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{ad.price.positive}")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotBlank(message = "{ad.city.notblank}")
    @Pattern(regexp = "^[A-ZА-ЯЁ][a-zа-яё]+$", message = "{ad.city.pattern}")
    @Column(nullable = false)
    private String city;

    @NotNull(message = "{ad.category.notnull}")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_promoted", nullable = false)
    private Boolean isPromoted = false;

    @Column(name = "promotion_end_date")
    private LocalDateTime promotionEndDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdStatus status = AdStatus.ACTIVE;

    @OneToMany(mappedBy = "ad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Comment> comments = new ArrayList<>();

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
