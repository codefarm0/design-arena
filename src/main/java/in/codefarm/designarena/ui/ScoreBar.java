package in.codefarm.designarena.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * A labelled progress bar: category name on the left, "2 / 3" on the right, a filled
 * track underneath.
 */
final class ScoreBar extends Div {

    ScoreBar(String label, int score, int maxScore, int percentage) {
        addClassName("da-bar-row");

        Span name = new Span(label);
        Span value = new Span("%d / %d".formatted(score, maxScore));
        value.addClassName("da-bar-row__value");

        Div labels = new Div(name, value);
        labels.addClassName("da-bar-row__labels");

        Div fill = new Div();
        fill.addClassName("da-bar__fill");
        // How full the bar is, is data. How the bar looks is design and stays in the
        // stylesheet, so Java sets a custom property and CSS decides what to do with it.
        fill.getStyle().set("--da-bar-fill", percentage + "%");

        Div track = new Div(fill);
        track.addClassName("da-bar");
        // The bar repeats information already in the text beside it, so it is decorative.
        track.getElement().setAttribute("aria-hidden", "true");

        add(labels, track);
    }
}
