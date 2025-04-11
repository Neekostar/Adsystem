package com.neekostar.adsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank(message = "{username.notblank}")
    @Size(min = 3, max = 50, message = "{username.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{username.pattern}")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "{password.notblank}")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "{email.notblank}")
    @Email(message = "{email.invalid}")
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Size(min = 3, max = 50, message = "{first_name.size}")
    @Column(name = "first_name", length = 50)
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{first_name.pattern}")
    private String firstName;

    @Size(min = 3, max = 50, message = "{last_name.size}")
    @Column(name = "last_name", length = 50)
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{last_name.pattern}")
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Min(value = 0, message = "{rating.min}")
    @Column(nullable = false)
    private Float rating = 0f;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
