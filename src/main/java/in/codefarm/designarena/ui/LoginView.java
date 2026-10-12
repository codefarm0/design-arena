package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Signing in.
 *
 * <p>There is no mockup for this screen — the approved designs cover the five screens of
 * the product itself — so it is built from the same tokens and the same card, label and
 * button patterns as everything else.
 *
 * <p>It is a plain HTML form posting to {@code /login} rather than a Vaadin component
 * sending the password over the websocket, because Spring Security's filter has to see
 * the request. That also means it keeps working with autofill and password managers.
 */
@Route(value = "login", layout = MainLayout.class)
@PageTitle("Sign in · Design Arena")
@AnonymousAllowed
public final class LoginView extends Div implements BeforeEnterObserver, HasHeaderActions {

    private final Div message = new Div();

    public LoginView() {
        addClassNames("da-view", "da-view--narrow");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        boolean failed =
                event.getLocation().getQueryParameters().getParameters().containsKey("error");
        add(heading(), form(failed));
    }

    @Override
    public Component headerActions() {
        Anchor register = new Anchor("signup", "Create an account");
        register.addClassName("da-nav__link");
        return register;
    }

    private Div heading() {
        H1 title = new H1("Sign in");
        title.addClassName("da-page-heading__title");

        Paragraph lead = new Paragraph("Signing in keeps your attempt history and your monthly review allowance.");
        lead.addClassName("da-hero__lead");

        Div heading = new Div(title, lead);
        heading.addClassName("da-page-heading");
        return heading;
    }

    private Section form(boolean failed) {
        Element form = new Element("form");
        form.setAttribute("method", "post");
        form.setAttribute("action", "login");
        form.getClassList().add("da-form");

        if (failed) {
            message.addClassNames("da-alert", "da-alert--inline");
            message.getElement().setAttribute("role", "alert");
            message.removeAll();
            Span text = new Span("That email and password did not match an account.");
            text.addClassName("da-alert__detail");
            message.add(text);
            form.appendChild(message.getElement());
        }

        form.appendChild(field("Email", "username", "email", "username").getElement());
        form.appendChild(
                field("Password", "password", "password", "current-password").getElement());

        NativeButton submit = new NativeButton("Sign in");
        submit.addClassNames("da-button", "da-button--primary");
        submit.getElement().setAttribute("type", "submit");
        form.appendChild(submit.getElement());

        Section card = new Section();
        card.addClassNames("da-card", "da-card--flat");
        card.getElement().appendChild(form);
        return card;
    }

    /** A labelled input. The label is a real {@code <label for>}, not a placeholder. */
    static Div field(String label, String name, String type, String autocomplete) {
        Input input = new Input();
        input.setId("field-" + name);
        input.setType(type);
        input.getElement().setAttribute("name", name);
        input.getElement().setAttribute("autocomplete", autocomplete);
        input.getElement().setAttribute("required", true);
        input.addClassName("da-input");

        NativeLabel caption = new NativeLabel(label);
        caption.addClassName("da-panel__label");
        caption.setFor(input);

        Div field = new Div(caption, input);
        field.addClassName("da-field");
        return field;
    }
}
