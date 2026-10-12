package in.codefarm.designarena.problem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** How a free-text design for one problem is judged. */
@Entity
@Table(name = "rubric")
class Rubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @OneToMany(mappedBy = "rubric", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<RubricDimension> dimensions = new ArrayList<>();

    protected Rubric() {
        // Required by JPA.
    }

    Rubric(Problem problem) {
        this.problem = problem;
    }

    void replaceDimensions(List<RubricDimension> replacements) {
        dimensions.clear();
        dimensions.addAll(replacements);
    }

    List<RubricDimension> getDimensions() {
        return dimensions;
    }
}
