package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

/**
 * The site header: the wordmark on the left, and whatever the current screen puts on the
 * right.
 *
 * <p>Exists once and is reused by every screen, because the mockups show the same header
 * everywhere with only the trailing content changing.
 */
final class AppHeader extends Header {

    private static final String LOGO = "images/logo.svg";

    private final Div inner;
    private Component trailing;

    AppHeader(Component initialTrailing) {
        addClassName("da-header");

        Image logo = new Image(LOGO, "");
        // Decorative: the wordmark beside it already names the product, so a screen
        // reader announcing the image too would just repeat itself.
        logo.getElement().setAttribute("aria-hidden", "true");

        Span wordmark = new Span("Design Arena");
        wordmark.addClassName("da-wordmark__text");

        Anchor home = new Anchor("/", logo, wordmark);
        home.addClassName("da-wordmark");
        home.getElement().setAttribute("aria-label", "Design Arena home");

        this.trailing = initialTrailing;
        this.inner = new Div(home, trailing);
        inner.addClassName("da-header__inner");
        add(inner);
    }

    /** Swaps what appears on the right when the routed screen asks for something else. */
    void setTrailing(Component replacement) {
        inner.replace(trailing, replacement);
        this.trailing = replacement;
    }
}
