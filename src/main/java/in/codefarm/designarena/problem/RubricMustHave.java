package in.codefarm.designarena.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A specific point a staff-level answer is expected to cover. */
@Entity
@Table(name = "rubric_must_have")
class RubricMustHave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rubric_dimension_id", nullable = false)
    private RubricDimension dimension;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private String text;

    protected RubricMustHave() {
        // Required by JPA.
    }

    RubricMustHave(RubricDimension dimension, int position, String text) {
        this.dimension = dimension;
        this.position = position;
        this.text = text;
    }

    String getText() {
        return text;
    }
}
