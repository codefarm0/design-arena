package in.codefarm.designarena.problem;

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

/** One bullet in the functional or non-functional requirement list. */
@Entity
@Table(name = "problem_requirement")
class ProblemRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementKind kind;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private String text;

    protected ProblemRequirement() {
        // Required by JPA.
    }

    ProblemRequirement(Problem problem, RequirementKind kind, int position, String text) {
        this.problem = problem;
        this.kind = kind;
        this.position = position;
        this.text = text;
    }

    RequirementKind getKind() {
        return kind;
    }

    String getText() {
        return text;
    }
}
