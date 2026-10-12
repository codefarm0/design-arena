package in.codefarm.designarena.problem;

import org.springframework.data.repository.Repository;

import java.util.Optional;

/** Reads rubrics. */
interface RubricRepository extends Repository<Rubric, Long> {

    Optional<Rubric> findByProblemId(long problemId);

    Rubric save(Rubric rubric);
}
