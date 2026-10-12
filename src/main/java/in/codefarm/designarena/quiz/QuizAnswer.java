package in.codefarm.designarena.quiz;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What the user selected for one question.
 *
 * <p>A multi-choice question can have several options selected, so the selection is its own
 * table rather than a delimited column.
 */
@Entity
@Table(name = "quiz_answer")
class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_answer_option", joinColumns = @JoinColumn(name = "quiz_answer_id"))
    @Column(name = "question_option_id", nullable = false)
    private Set<Long> selectedOptionIds = new LinkedHashSet<>();

    protected QuizAnswer() {
        // Required by JPA.
    }

    QuizAnswer(QuizAttempt attempt, Long questionId, Set<Long> selectedOptionIds) {
        this.attempt = attempt;
        this.questionId = questionId;
        this.selectedOptionIds = new LinkedHashSet<>(selectedOptionIds);
    }

    Long getQuestionId() {
        return questionId;
    }

    Set<Long> getSelectedOptionIds() {
        return Set.copyOf(selectedOptionIds);
    }

    void replaceSelection(Set<Long> optionIds) {
        selectedOptionIds.clear();
        selectedOptionIds.addAll(optionIds);
    }
}
