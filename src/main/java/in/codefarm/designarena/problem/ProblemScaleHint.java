package in.codefarm.designarena.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One scale figure, such as "10M daily active users". */
@Entity
@Table(name = "problem_scale_hint")
class ProblemScaleHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private String text;

    protected ProblemScaleHint() {
        // Required by JPA.
    }

    ProblemScaleHint(Problem problem, int position, String text) {
        this.problem = problem;
        this.position = position;
        this.text = text;
    }

    String getText() {
        return text;
    }
}
