package in.codefarm.designarena.quiz;

/**
 * How many questions one problem has.
 *
 * @param problemId the problem the questions belong to
 * @param total the number of questions
 */
record ProblemQuestionCount(Long problemId, Long total) {}
