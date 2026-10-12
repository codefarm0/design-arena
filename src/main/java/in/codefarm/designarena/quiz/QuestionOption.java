package in.codefarm.designarena.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One answer a user can pick.
 *
 * <p>Every option carries its own explanation, including the wrong ones. That is what lets
 * the results page say why an option is wrong rather than only naming the right one.
 */
@Entity
@Table(name = "question_option")
class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private String explanation;

    protected QuestionOption() {
        // Required by JPA.
    }

    QuestionOption(Question question, int position, String text, boolean correct, String explanation) {
        this.question = question;
        this.position = position;
        this.text = text;
        this.correct = correct;
        this.explanation = explanation;
    }

    Long getId() {
        return id;
    }

    int getPosition() {
        return position;
    }

    String getText() {
        return text;
    }

    boolean isCorrect() {
        return correct;
    }

    String getExplanation() {
        return explanation;
    }
}
