package in.codefarm.designarena.review;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;

/**
 * One attempt to review one submission, and the evidence of how it went.
 *
 * <p>The result is stored as JSON rather than as columns because it is the model's answer,
 * not our data model: changing the shape of a review later must not invalidate reviews
 * already produced. The raw response is kept even on success — it is the only way to
 * explain a strange review after the prompt has moved on.
 */
@Entity
@Table(name = "evaluation")
class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private DesignSubmission submission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "raw_response")
    private String rawResponse;

    private String provider;

    private String model;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Evaluation() {
        // Required by JPA.
    }

    Evaluation(DesignSubmission submission, Instant createdAt) {
        this.submission = submission;
        this.status = EvaluationStatus.PENDING;
        this.createdAt = createdAt;
    }

    Long getId() {
        return id;
    }

    DesignSubmission getSubmission() {
        return submission;
    }

    EvaluationStatus getStatus() {
        return status;
    }

    Optional<String> getResultJson() {
        return Optional.ofNullable(resultJson);
    }

    String getProvider() {
        return provider;
    }

    String getModel() {
        return model;
    }

    String getPromptVersion() {
        return promptVersion;
    }

    Optional<String> getFailureReason() {
        return Optional.ofNullable(failureReason);
    }

    void recordUsage(Integer promptTokens, Integer completionTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    void succeeded(
            String resultJson,
            String rawResponse,
            String provider,
            String model,
            String promptVersion,
            long latencyMs,
            Instant at) {
        this.status = EvaluationStatus.DONE;
        this.resultJson = resultJson;
        this.rawResponse = rawResponse;
        this.provider = provider;
        this.model = model;
        this.promptVersion = promptVersion;
        this.latencyMs = latencyMs;
        this.completedAt = at;
        this.failureReason = null;
    }

    void failed(
            String rawResponse,
            String provider,
            String model,
            String promptVersion,
            long latencyMs,
            String reason,
            Instant at) {
        this.status = EvaluationStatus.FAILED;
        this.rawResponse = rawResponse;
        this.provider = provider;
        this.model = model;
        this.promptVersion = promptVersion;
        this.latencyMs = latencyMs;
        this.failureReason = reason;
        this.completedAt = at;
    }

    void reopened() {
        this.status = EvaluationStatus.PENDING;
        this.failureReason = null;
        this.completedAt = null;
    }
}
