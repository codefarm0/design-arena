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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import in.codefarm.designarena.user.Accounts;
import in.codefarm.designarena.user.EmailAlreadyRegisteredException;

/**
 * Creating an account.
 *
 * <p>The milestone asked for login; an application where nobody can create an account has
 * a login nobody can use, so registration comes with it. It is deliberately the smallest
 * thing that works: email, a display name, a password. No email confirmation yet, which
 * is noted here because it is a real gap rather than an oversight.
 */
@Route(value = "signup", layout = MainLayout.class)
@PageTitle("Create an account · Design Arena")
@AnonymousAllowed
public final class SignUpView extends Div implements HasHeaderActions {

    /** Short enough not to be annoying, long enough to be worth hashing. */
    private static final int MINIMUM_PASSWORD_LENGTH = 10;

    private final transient Accounts accounts;

    private final Input email = textInput("email", "email", "email");
    private final Input displayName = textInput("displayName", "text", "name");
    private final Input password = textInput("password", "password", "new-password");
    private final Span message = new Span();

    public SignUpView(Accounts accounts) {
        this.accounts = accounts;
        addClassNames("da-view", "da-view--narrow");

        H1 title = new H1("Create an account");
        title.addClassName("da-page-heading__title");
        Paragraph lead = new Paragraph("Free accounts include three design reviews a month. The quiz is unlimited.");
        lead.addClassName("da-hero__lead");
        Div heading = new Div(title, lead);
        heading.addClassName("da-page-heading");

        add(heading, form());
    }

    @Override
    public Component headerActions() {
        Anchor signIn = new Anchor("login", "Sign in");
        signIn.addClassName("da-nav__link");
        return signIn;
    }

    private Section form() {
        NativeButton submit = new NativeButton("Create account", event -> register());
        submit.addClassNames("da-button", "da-button--primary");

        message.addClassName("da-caption");
        message.getElement().setAttribute("role", "status");

        Div fields = new Div(
                labelled("Email", email),
                labelled("Display name", displayName),
                labelled("Password (at least %d characters)".formatted(MINIMUM_PASSWORD_LENGTH), password),
                message,
                submit);
        fields.addClassName("da-form");

        Section card = new Section(fields);
        card.addClassNames("da-card", "da-card--flat");
        return card;
    }

    private void register() {
        if (password.getValue().length() < MINIMUM_PASSWORD_LENGTH) {
            show("Use at least %d characters for the password.".formatted(MINIMUM_PASSWORD_LENGTH));
            return;
        }
        try {
            accounts.register(email.getValue(), displayName.getValue(), password.getValue());
            // Straight to sign-in rather than logging them in here: authentication is
            // Spring Security's job, and a form post is the one path it sees.
            getUI().ifPresent(ui -> ui.navigate("login"));
        } catch (EmailAlreadyRegisteredException e) {
            show("There is already an account for that email. Try signing in instead.");
        }
    }

    private void show(String text) {
        message.setText(text);
        message.getElement().setAttribute("role", "alert");
    }

    private static Input textInput(String name, String type, String autocomplete) {
        Input input = new Input();
        input.setId("field-" + name);
        input.setType(type);
        input.getElement().setAttribute("autocomplete", autocomplete);
        input.addClassName("da-input");
        return input;
    }

    private static Div labelled(String label, Input input) {
        NativeLabel caption = new NativeLabel(label);
        caption.addClassName("da-panel__label");
        caption.setFor(input);

        Div field = new Div(caption, input);
        field.addClassName("da-field");
        return field;
    }
}
