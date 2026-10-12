package in.codefarm.designarena.quiz;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** One person's run through the questions for a problem. */
@Entity
@Table(name = "quiz_attempt")
class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    private Integer score;

    @Column(name = "max_score")
    private Integer maxScore;

    /** The AI narrative. Null is a valid state: the results page renders without it. */
    @Column(name = "summary_text")
    private String summaryText;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAnswer> answers = new ArrayList<>();

    protected QuizAttempt() {
        // Required by JPA.
    }

    QuizAttempt(Long userId, Long problemId, Instant startedAt) {
        this.userId = userId;
        this.problemId = problemId;
        this.startedAt = startedAt;
    }

    Long getId() {
        return id;
    }

    Long getProblemId() {
        return problemId;
    }

    boolean isSubmitted() {
        return submittedAt != null;
    }

    List<QuizAnswer> getAnswers() {
        return answers;
    }

    Optional<String> getSummaryText() {
        return Optional.ofNullable(summaryText);
    }

    void addAnswer(QuizAnswer answer) {
        answers.add(answer);
    }

    void submit(Instant at, int scored, int outOf) {
        this.submittedAt = at;
        this.score = scored;
        this.maxScore = outOf;
    }

    void attachSummary(String summary) {
        this.summaryText = summary;
    }
}
