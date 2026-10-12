package in.codefarm.designarena.ui;

import com.vaadin.flow.component.html.Span;

/** The small pill used for difficulty, question category and question type. */
final class Chip extends Span {

    Chip(String text) {
        super(text);
        addClassName("da-chip");
    }

    /** The brand-tinted variant, used to mark content a model produced. */
    static Chip brand(String text) {
        Chip chip = new Chip(text);
        chip.addClassName("da-chip--brand");
        return chip;
    }
}
