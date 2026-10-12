package in.codefarm.designarena.quiz;

/**
 * The editable shape of one option.
 *
 * @param text the option text
 * @param correct whether selecting it is right
 * @param explanation why it is right or wrong — required, because it is the product
 */
public record OptionDraft(String text, boolean correct, String explanation) {}
