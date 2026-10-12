package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemDetail;
import in.codefarm.designarena.problem.ProblemNotFoundException;
import in.codefarm.designarena.review.Allowance;
import in.codefarm.designarena.review.ReviewLimitReachedException;
import in.codefarm.designarena.review.ReviewService;
import in.codefarm.designarena.review.SubmissionTooLongException;
import in.codefarm.designarena.user.Accounts;

import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.Map;

/**
 * Where a design is written: the problem on the left, a Markdown editor and an optional
 * Mermaid diagram on the right.
 *
 * <p>Built to {@code docs/ui/ui/Editor.html}.
 */
@Route(value = "design/:slug", layout = MainLayout.class)
@PageTitle("Design editor · Design Arena")
@PermitAll
public final class DesignEditorView extends Div implements BeforeEnterObserver, HasHeaderActions {

    /**
     * The headings a design is expected to cover, pre-filled into the editor.
     *
     * <p>A blank page is the hardest part of a design interview. The template is the same
     * shape as the rubric, so the structure itself is a hint.
     */
    private static final String TEMPLATE = """
            ## Requirements

            ## API

            ## Data model

            ## High-level design

            ## Scaling

            ## Failure handling

            ## Trade-offs
            """;

    private final transient ReviewService reviews;
    private final transient ProblemCatalogue problems;
    private final transient Accounts accounts;

    private final TextArea design = new TextArea();
    private final TextArea diagram = new TextArea();
    private final Span counter = new Span();
    private final MermaidPreview preview = new MermaidPreview();
    private final Span status = new Span();

    private ProblemDetail problem;

    public DesignEditorView(ReviewService reviews, ProblemCatalogue problems, Accounts accounts) {
        this.reviews = reviews;
        this.problems = problems;
        this.accounts = accounts;
        addClassNames("da-view", "da-view--wide");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String slug = event.getRouteParameters().get("slug").orElse("");
        try {
            problem = problems.detailBySlug(slug);
        } catch (ProblemNotFoundException e) {
            event.forwardTo(ProblemCatalogueView.class);
            return;
        }
        render();
    }

    @Override
    public Component headerActions() {
        status.addClassName("da-meta");
        Div actions = new Div(status, submitButton("Submit for review"));
        actions.addClassName("da-quiz-actions__right");
        return actions;
    }

    private void render() {
        removeAll();
        Div editor = new Div(brief(), editorColumn());
        editor.addClassName("da-editor");
        add(editor);
    }

    private Aside brief() {
        Span eyebrow = new Span("PROBLEM · " + problem.difficulty().name());
        eyebrow.addClassName("da-brief__eyebrow");

        H1 title = new H1(problem.title());
        title.addClassName("da-brief__title");

        Paragraph statement = new Paragraph(problem.statementMarkdown());
        statement.addClassName("da-brief__statement");

        Aside aside = new Aside(eyebrow, title, statement);
        aside.addClassName("da-brief");
        aside.add(requirementList("Functional requirements", problem.functionalRequirements()));
        aside.add(requirementList("Non-functional requirements", problem.nonFunctionalRequirements()));
        aside.add(scaleHints());

        Anchor learn = new Anchor("https://codefarm.in", "Brush up on the concepts at codefarm.in →");
        learn.getElement().getStyle().remove("font-size");
        aside.add(learn);
        return aside;
    }

    private static Div requirementList(String heading, List<String> items) {
        H2 title = new H2(heading);
        title.addClassName("da-brief__heading");

        UnorderedList list = new UnorderedList();
        list.addClassName("da-brief__list");
        items.forEach(item -> list.add(new ListItem(item)));

        Div group = new Div(title, list);
        group.addClassName("da-brief__group");
        return group;
    }

    private Div scaleHints() {
        H2 title = new H2("Scale hints");
        title.addClassName("da-brief__heading");

        Div panel = new Div(title);
        panel.addClassName("da-brief__scale");
        problem.scaleHints().forEach(hint -> {
            Span figure = new Span(hint);
            figure.addClassName("da-brief__figure");
            panel.add(figure);
        });
        return panel;
    }

    private Div editorColumn() {
        Div column = new Div(designPanel(), diagramPanel(), footer());
        column.addClassName("da-editor__main");
        return column;
    }

    private Div designPanel() {
        design.setValue(TEMPLATE);
        design.addClassNames("da-code-area", "da-code-area--design");
        design.setValueChangeMode(ValueChangeMode.EAGER);
        design.setWidthFull();
        design.addValueChangeListener(event -> updateCounter());

        NativeLabel label = new NativeLabel("Your design ");
        label.addClassName("da-panel__label");
        Span hint = new Span("(Markdown)");
        hint.addClassName("da-panel__label-hint");
        label.add(hint);
        label.setFor(design);

        updateCounter();
        Div header = new Div(label, counter);
        header.addClassName("da-panel__header");

        Div panel = new Div(header, design);
        panel.addClassName("da-panel");
        return panel;
    }

    private void updateCounter() {
        int length = design.getValue() == null ? 0 : design.getValue().length();
        counter.setText("%,d / %,d".formatted(length, ReviewService.MAX_SUBMISSION_LENGTH));
        counter.getElement().getClassList().set("da-panel__counter", true);
        counter.getElement()
                .getClassList()
                .set("da-panel__counter--over", length > ReviewService.MAX_SUBMISSION_LENGTH);
    }

    private Div diagramPanel() {
        diagram.addClassNames("da-code-area", "da-code-area--diagram");
        diagram.setValueChangeMode(ValueChangeMode.LAZY);
        diagram.setPlaceholder("flowchart LR\n  C[Client] --> G[API Gateway]");
        diagram.addValueChangeListener(event -> preview.setCode(event.getValue()));

        NativeLabel label = new NativeLabel("Diagram ");
        label.addClassName("da-panel__label");
        Span hint = new Span("(Mermaid, optional)");
        hint.addClassName("da-panel__label-hint");
        label.add(hint);
        label.setFor(diagram);

        Anchor help = new Anchor("https://mermaid.js.org/intro/syntax-reference.html", "Mermaid syntax help");
        help.setTarget("_blank");

        Div header = new Div(label, help);
        header.addClassName("da-panel__header");

        Span previewLabel = new Span("Live preview");
        previewLabel.addClassName("da-preview__label");
        Div previewPane = new Div(previewLabel, preview);
        previewPane.addClassName("da-preview");

        Div split = new Div(diagram, previewPane);
        split.addClassName("da-panel__split");

        Div panel = new Div(header, split);
        panel.addClassName("da-panel");
        return panel;
    }

    private Div footer() {
        Span note = new Span(allowanceText() + " Reviews take a minute or two; the result keeps its own link.");
        note.addClassName("da-meta");

        Div footer = new Div(note, submitButton("Submit for review"));
        footer.addClassName("da-editor__footer");
        return footer;
    }

    /**
     * What this person has left this month.
     *
     * <p>Shown before they write rather than after they submit: finding out that a review
     * was never going to run is worth knowing at the top of the page.
     */
    private String allowanceText() {
        Allowance allowance = reviews.allowanceFor(accounts.currentOrFail());
        if (!allowance.capped()) {
            return "Your plan has no monthly limit.";
        }
        int remaining = allowance.remaining().orElse(0);
        return remaining == 1
                ? "1 design review left this month."
                : "%d design reviews left this month.".formatted(remaining);
    }

    private NativeButton submitButton(String label) {
        NativeButton submit = new NativeButton(label, event -> submit());
        submit.addClassNames("da-button", "da-button--primary");
        return submit;
    }

    private void submit() {
        try {
            long submissionId = reviews.submit(problem.slug(), design.getValue(), diagram.getValue());
            RouteParameters route = new RouteParameters(Map.of("submissionId", String.valueOf(submissionId)));
            getUI().ifPresent(ui -> ui.navigate(DesignReviewView.class, route));
        } catch (SubmissionTooLongException e) {
            // Validation at the boundary: the user hears about it before a model is paid
            // for, and the message says what to do rather than what went wrong inside.
            warn("Your design is too long to review. Trim it to %,d characters."
                    .formatted(ReviewService.MAX_SUBMISSION_LENGTH));
        } catch (ReviewLimitReachedException e) {
            warn("You have used all %d design reviews on the free plan this month. Your design is still here."
                    .formatted(e.limit()));
        }
    }

    private void warn(String text) {
        status.setText(text);
        status.getElement().setAttribute("role", "alert");
        status.getElement().getClassList().set("da-status--warning", true);
    }
}
