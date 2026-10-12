package in.codefarm.designarena.ui;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import in.codefarm.designarena.user.Account;
import in.codefarm.designarena.user.Accounts;

/**
 * The frame every screen is rendered inside: header, then the routed view inside a
 * {@code <main>} element that owns the page width and gutters.
 *
 * <p>The layout is annotated {@code @AnonymousAllowed} because it is a frame, not a
 * screen: each view inside it still declares its own rule. Vaadin refuses to let a view
 * be more permissive than its layout, so leaving this off denied every page — including
 * the public catalogue — with a helpfully explicit warning in the log.
 *
 * <p>This is also where the design system is loaded. Vaadin 25 deprecated the
 * {@code @Theme} annotation in favour of importing stylesheets directly, so the single
 * token stylesheet is imported here, on the layout every screen is rendered inside.
 */
@CssImport("./styles/design-arena.css")
@AnonymousAllowed
public final class MainLayout extends Div implements RouterLayout {

    private final transient Accounts accounts;
    private final transient AuthenticationContext authentication;

    private final Main content = new Main();
    private final AppHeader header;

    public MainLayout(Accounts accounts, AuthenticationContext authentication) {
        this.accounts = accounts;
        this.authentication = authentication;
        this.header = new AppHeader(navigation());

        addClassName("da-page");
        content.addClassName("da-main");
        add(header, content);
    }

    /**
     * The default navigation, which differs for a signed-in person.
     *
     * <p>History only appears once there is history to show, and "Sign in" becomes the
     * person's name and a sign-out button.
     */
    private Nav navigation() {
        Nav nav = new Nav();
        nav.addClassName("da-nav");
        nav.getElement().setAttribute("aria-label", "Main");

        Anchor practice = new Anchor("/", "Practice");
        practice.addClassName("da-nav__link");
        practice.getElement().setAttribute("data-current", "");
        nav.add(practice);

        accounts.current().ifPresent(account -> {
            nav.add(historyLink());
            if (account.isAdmin()) {
                nav.add(adminLink());
            }
        });

        Anchor learn = new Anchor("https://codefarm.in", "Learn on codefarm.in");
        learn.addClassName("da-nav__link");
        nav.add(learn);

        accounts.current().ifPresentOrElse(account -> nav.add(signedIn(account)), () -> nav.add(signInLink()));
        return nav;
    }

    private static Anchor historyLink() {
        Anchor history = new Anchor("history", "History");
        history.addClassName("da-nav__link");
        return history;
    }

    private static Anchor adminLink() {
        Anchor admin = new Anchor("admin", "Admin");
        admin.addClassName("da-nav__link");
        return admin;
    }

    private static Anchor signInLink() {
        Anchor signIn = new Anchor("login", "Sign in");
        signIn.addClassNames("da-button", "da-button--secondary");
        return signIn;
    }

    private Div signedIn(Account account) {
        Span who = new Span(account.displayName());
        who.addClassName("da-nav__who");

        // Sign-out goes through Spring Security rather than clearing anything here, so
        // the session is invalidated server-side and not merely forgotten by the UI.
        NativeButton signOut = new NativeButton("Sign out", event -> authentication.logout());
        signOut.addClassNames("da-button", "da-button--secondary");

        Div group = new Div(who, signOut);
        group.addClassName("da-nav__account");
        return group;
    }

    @Override
    public void showRouterLayoutContent(HasElement newContent) {
        content.getElement().removeAllChildren();
        content.getElement().appendChild(newContent.getElement());
        // The header is the same component on every screen; only its right-hand side
        // changes, and the screen itself says what it should be.
        header.setTrailing(newContent instanceof HasHeaderActions view ? view.headerActions() : navigation());
    }
}
