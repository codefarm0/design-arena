package in.codefarm.designarena.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A design somebody wrote, exactly as they wrote it. */
@Entity
@Table(name = "design_submission")
class DesignSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "content_md", nullable = false)
    private String contentMarkdown;

    @Column(name = "diagram_mermaid")
    private String diagramMermaid;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DesignSubmission() {
        // Required by JPA.
    }

    DesignSubmission(Long userId, Long problemId, String contentMarkdown, String diagramMermaid, Instant createdAt) {
        this.userId = userId;
        this.problemId = problemId;
        this.contentMarkdown = contentMarkdown;
        this.diagramMermaid = diagramMermaid;
        this.createdAt = createdAt;
    }

    Long getId() {
        return id;
    }

    Long getUserId() {
        return userId;
    }

    Long getProblemId() {
        return problemId;
    }

    String getContentMarkdown() {
        return contentMarkdown;
    }

    String getDiagramMermaid() {
        return diagramMermaid == null ? "" : diagramMermaid;
    }
}
