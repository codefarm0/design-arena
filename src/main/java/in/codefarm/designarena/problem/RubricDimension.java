package in.codefarm.designarena.problem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** One thing a design is judged on, such as "Reliability and failure modes". */
@Entity
@Table(name = "rubric_dimension")
class RubricDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rubric_id", nullable = false)
    private Rubric rubric;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int weight;

    @OneToMany(mappedBy = "dimension", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<RubricMustHave> mustHaves = new ArrayList<>();

    protected RubricDimension() {
        // Required by JPA.
    }

    RubricDimension(Rubric rubric, int position, String name, String description, int weight) {
        this.rubric = rubric;
        this.position = position;
        this.name = name;
        this.description = description;
        this.weight = weight;
    }

    void replaceMustHaves(List<RubricMustHave> replacements) {
        mustHaves.clear();
        mustHaves.addAll(replacements);
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    int getWeight() {
        return weight;
    }

    List<RubricMustHave> getMustHaves() {
        return mustHaves;
    }
}
