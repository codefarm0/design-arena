package in.codefarm.designarena.ai;

/**
 * The model's score for one rubric dimension.
 *
 * @param dimension the dimension name, matching one the rubric asked about
 * @param score out of ten
 * @param comment one sentence of justification
 */
public record DimensionScore(String dimension, int score, String comment) {}
