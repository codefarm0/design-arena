package in.codefarm.designarena.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A registered person, including the one field nothing outside this package may see. */
@Entity
@Table(name = "app_user")
class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUser() {
        // Required by JPA.
    }

    AppUser(String email, String displayName, String passwordHash, Instant createdAt) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.plan = UserPlan.FREE;
        this.role = UserRole.USER;
        this.createdAt = createdAt;
    }

    Long getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getDisplayName() {
        return displayName;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    UserPlan getPlan() {
        return plan;
    }

    UserRole getRole() {
        return role;
    }

    void grantAdmin() {
        this.role = UserRole.ADMIN;
    }

    Account toAccount() {
        return new Account(id, email, displayName, plan, role);
    }
}
