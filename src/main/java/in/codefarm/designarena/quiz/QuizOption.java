package in.codefarm.designarena.quiz;

/**
 * One selectable answer, as the UI needs it. Deliberately does not carry {@code correct}
 * or the explanation: nothing that would give the answer away is sent to the browser
 * before the attempt is submitted.
 *
 * @param id the option's identity, used when recording the answer
 * @param key the letter shown beside it (A, B, C...)
 * @param text the option text
 */
public record QuizOption(long id, String key, String text) {}
