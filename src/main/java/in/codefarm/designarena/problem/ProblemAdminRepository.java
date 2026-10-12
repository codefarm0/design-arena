package in.codefarm.designarena.problem;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Reads and writes problems for the admin area.
 *
 * <p>Separate from {@link ProblemRepository} because it needs what the catalogue must not
 * have: whole entities, inactive rows, and the ability to write. Keeping the write
 * methods out of the repository the public screens use means a reader can see at a glance
 * that the catalogue cannot modify anything.
 */
interface ProblemAdminRepository extends Repository<Problem, Long> {

    Problem save(Problem problem);

    Optional<Problem> findBySlug(String slug);

    /** Every problem, active or not, in catalogue order. There are tens of these, not millions. */
    List<Problem> findAllByOrderByPositionAsc();
}
