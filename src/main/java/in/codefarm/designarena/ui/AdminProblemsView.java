package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import in.codefarm.designarena.problem.ProblemAdmin;
import in.codefarm.designarena.problem.ProblemSummary;
import in.codefarm.designarena.problem.SlugAlreadyUsedException;

import jakarta.annotation.security.RolesAllowed;

/**
 * The admin landing page: every problem, published or not.
 *
 * <p>Guarded by {@code @RolesAllowed("ADMIN")}, which is the first thing in this
 * application to need a role rather than a plan.
 */
@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin · Design Arena")
@RolesAllowed("ADMIN")
public final class AdminProblemsView extends Div implements HasHeaderActions {

    private final transient ProblemAdmin admin;
    private final TextField newSlug = new TextField();
    private final Span message = new Span();

    public AdminProblemsView(ProblemAdmin admin) {
        this.admin = admin;
        addClassNames("da-view", "da-view--results");
        render();
    }

    @Override
    public Component headerActions() {
        Div actions =
                new Div(adminLink("Problems", "admin"), adminLink("AI status", "admin/ai"), adminLink("Practice", "/"));
        actions.addClassName("da-nav");
        return actions;
    }

    private static Anchor adminLink(String label, String href) {
        Anchor link = new Anchor(href, label);
        link.addClassName("da-nav__link");
        return link;
    }

    private void render() {
        removeAll();

        H1 title = new H1("Problems");
        title.addClassName("da-page-heading__title");
        Paragraph lead = new Paragraph("Everything in the catalogue, including problems that are not published yet.");
        lead.addClassName("da-hero__lead");
        Div heading = new Div(title, lead);
        heading.addClassName("da-page-heading");

        add(heading, createPanel());

        Div list = new Div();
        list.addClassName("da-results-list");
        admin.all().forEach(problem -> list.add(row(problem)));
        add(list);
    }

    private Section createPanel() {
        newSlug.setPlaceholder("new-problem-slug");
        newSlug.setLabel("Slug");
        newSlug.setHelperText("Lower case, hyphenated. It becomes the URL and cannot be changed later.");

        NativeButton create = new NativeButton("Create problem", event -> create());
        create.addClassNames("da-button", "da-button--primary");

        message.addClassName("da-caption");
        message.getElement().setAttribute("role", "status");

        Div form = new Div(newSlug, create, message);
        form.addClassName("da-form");

        Section card = new Section(form);
        card.addClassNames("da-card", "da-card--flat");
        return card;
    }

    private void create() {
        String slug = newSlug.getValue() == null ? "" : newSlug.getValue().strip();
        if (slug.isBlank()) {
            message.setText("Give the problem a slug first.");
            return;
        }
        try {
            admin.create(slug);
            newSlug.clear();
            render();
        } catch (SlugAlreadyUsedException e) {
            message.setText("That slug is taken. Pick another.");
        }
    }

    private Div row(ProblemSummary problem) {
        Span title = new Span(problem.title());
        title.addClassName("da-result__question");
        Span slug = new Span(problem.slug());
        slug.addClassName("da-meta");

        Span state = new Span(admin.isActive(problem.slug()) ? "PUBLISHED" : "DRAFT");
        state.addClassName("da-history__outcome");

        Anchor edit = new Anchor("admin/problem/" + problem.slug(), "Edit");
        edit.addClassNames("da-button", "da-button--secondary");

        Anchor questions = new Anchor("admin/problem/" + problem.slug() + "/questions", "Questions");
        questions.addClassNames("da-button", "da-button--secondary");

        Div actions = new Div(edit, questions);
        actions.addClassName("da-provenance__actions");

        Div row = new Div(new Div(title, slug), state, actions);
        row.addClassName("da-history__row");
        return row;
    }
}
