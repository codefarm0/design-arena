package in.codefarm.designarena.quiz;

/**
 * One option as scoring sees it.
 *
 * @param id the option's identity
 * @param text the option text
 * @param correct whether selecting it is right
 * @param explanation why it is right or wrong
 */
record ScorableOption(long id, String text, boolean correct, String explanation) {}
