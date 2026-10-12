package in.codefarm.designarena.quiz;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * One multiple-choice question about a problem.
 *
 * <p>The problem is held as a plain id rather than a {@code @ManyToOne}: the {@code Problem}
 * entity is package-private to its own feature, so the relation cannot cross the boundary.
 * That is the intended outcome — the quiz feature correlates by id and asks the problem
 * feature for anything else.
 */
@Entity
@Table(name = "question")
class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(nullable = false)
    private String category;

    /**
     * Options are part of the question and are never used without it, so they load with it
     * and are ordered by the database rather than by whatever order the rows come back in.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuestionOption> options = new ArrayList<>();

    protected Question() {
        // Required by JPA.
    }

    Question(Long problemId, int position) {
        this.problemId = problemId;
        this.position = position;
    }

    void apply(int newPosition, String newText, QuestionType newType, String newCategory) {
        this.position = newPosition;
        this.text = newText;
        this.type = newType;
        this.category = newCategory;
    }

    void replaceOptions(List<QuestionOption> replacements) {
        options.clear();
        options.addAll(replacements);
    }

    Long getId() {
        return id;
    }

    Long getProblemId() {
        return problemId;
    }

    int getPosition() {
        return position;
    }

    String getText() {
        return text;
    }

    QuestionType getType() {
        return type;
    }

    String getCategory() {
        return category;
    }

    List<QuestionOption> getOptions() {
        return List.copyOf(options);
    }
}
