package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import in.codefarm.designarena.ai.DesignEvaluation;
import in.codefarm.designarena.ai.DimensionScore;
import in.codefarm.designarena.review.EvaluationStatus;
import in.codefarm.designarena.review.ReviewService;
import in.codefarm.designarena.review.ReviewState;
import in.codefarm.designarena.review.SubmissionNotFoundException;

import jakarta.annotation.security.PermitAll;

import java.util.List;

/**
 * The review of one design, in whichever of its three states it is in.
 *
 * <p>Built to {@code docs/ui/ui/Review.html}, which draws all three: waiting, reviewed,
 * and failed. The failed state is a first-class screen rather than an error page, because
 * a model that will not produce valid output is an ordinary event, and the design the
 * user wrote is still there.
 */
@Route(value = "review/:submissionId", layout = MainLayout.class)
@PageTitle("Design review · Design Arena")
@PermitAll
public final class DesignReviewView extends Div implements BeforeEnterObserver, HasHeaderActions {

    private static final int POLL_INTERVAL_MS = 3_000;

    /** Three seconds apart for five minutes: a local model is slow, but not endless. */
    private static final int POLL_ATTEMPTS = 100;

    private final ReviewService reviews;
    private ReviewState state;
    private int pollsRemaining = POLL_ATTEMPTS;
    private Registration pollRegistration;

    /**
     * Set once we have stopped waiting for a PENDING review.
     *
     * <p>Without this the page would keep showing a spinner forever for a review that
     * will never finish — a worker that died, or an application restarted while the model
     * was still thinking. A spinner that never resolves is worse than an honest dead end
     * with a button.
     */
    private boolean gaveUpWaiting;

    public DesignReviewView(ReviewService reviews) {
        this.reviews = reviews;
        addClassNames("da-view", "da-view--results");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        long submissionId = event.getRouteParameters().getLong("submissionId").orElse(-1L);
        try {
            state = reviews.state(submissionId);
        } catch (SubmissionNotFoundException e) {
            event.forwardTo(ProblemCatalogueView.class);
            return;
        }
        render();
    }

    @Override
    public Component headerActions() {
        Anchor back = new Anchor("/", "Back to problems");
        back.addClassName("da-nav__link");
        return back;
    }

    private void render() {
        removeAll();
        add(heading());
        // A switch expression rather than a statement: it has to be exhaustive, so adding
        // a fourth status becomes a compile error here instead of a silently missing
        // screen. That is also why there is no default branch to fall into.
        add(
                switch (state.status()) {
                    case PENDING -> waiting();
                    case FAILED -> failure();
                    case DONE -> review();
                });
        if (state.status() == EvaluationStatus.PENDING && !gaveUpWaiting) {
            awaitResult();
        }
    }

    private Div heading() {
        Span where = new Span(state.problemTitle() + " · Design review");
        where.addClassName("da-meta");

        H1 title = new H1("Your design review");
        title.addClassName("da-page-heading__title");

        Div heading = new Div(where, title);
        heading.addClassName("da-page-heading");
        return heading;
    }

    // --- PENDING -----------------------------------------------------------

    private Section waiting() {
        Div spinner = new Div();
        spinner.addClassName("da-spinner");
        spinner.getElement().setAttribute("aria-hidden", "true");

        Span title = new Span("Reviewing your design…");
        title.addClassName("da-state__title");

        Span detail = new Span("Checking it against the rubric. This usually takes a minute or two. "
                + "You can leave this page; the result keeps this link.");
        detail.addClassName("da-state__detail");

        Section section = new Section(spinner, title, detail);
        section.addClassName("da-state");
        // Announced when it changes, without stealing focus from the page.
        section.getElement().setAttribute("role", "status");
        return section;
    }

    private void awaitResult() {
        UI ui = UI.getCurrent();
        ui.setPollInterval(POLL_INTERVAL_MS);
        pollRegistration = ui.addPollListener(event -> {
            ReviewState latest = reviews.state(state.submissionId());
            boolean finished = latest.status() != EvaluationStatus.PENDING;
            if (finished || --pollsRemaining <= 0) {
                state = latest;
                gaveUpWaiting = !finished;
                stopPolling();
                render();
            }
        });
        addDetachListener(event -> stopPolling());
    }

    private void stopPolling() {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        getUI().ifPresent(ui -> ui.setPollInterval(-1));
    }

    /**
     * Still PENDING after we stopped waiting. The review may yet complete, so the design
     * is untouched and the two useful actions are to reload or to review it again.
     */
    private Section stalled() {
        Span title = new Span("This is taking longer than expected");
        title.addClassName("da-alert__title");

        Span detail = new Span(
                "The review has not come back yet. Your design is saved — reload this page, or start the review again.");
        detail.addClassName("da-alert__detail");

        Div text = new Div(title, detail);
        text.addClassName("da-banner__text");

        Section alert = new Section(text, retryButton("Review it again"));
        alert.addClassName("da-alert");
        alert.getElement().setAttribute("role", "alert");
        return alert;
    }

    // --- FAILED ------------------------------------------------------------

    private Section failure() {
        Span title = new Span("The review didn't finish");
        title.addClassName("da-alert__title");

        Span detail =
                new Span("The model returned a response we couldn't read, even after retrying. Your design is saved.");
        detail.addClassName("da-alert__detail");

        Div text = new Div(title, detail);
        text.addClassName("da-banner__text");

        Section alert = new Section(text, retryButton("Retry review"));
        alert.addClassName("da-alert");
        alert.getElement().setAttribute("role", "alert");
        return alert;
    }

    private NativeButton retryButton(String label) {
        NativeButton retry = new NativeButton(label, event -> {
            reviews.retry(state.submissionId());
            state = reviews.state(state.submissionId());
            pollsRemaining = POLL_ATTEMPTS;
            gaveUpWaiting = false;
            render();
        });
        retry.addClassNames("da-button", "da-button--danger");
        return retry;
    }

    // --- DONE --------------------------------------------------------------

    private Div review() {
        DesignEvaluation evaluation = state.evaluation().orElseThrow();
        Div done = new Div(scoreSection(evaluation), findings(evaluation), submission(), provenance());
        done.addClassName("da-view");
        return done;
    }

    private Section scoreSection(DesignEvaluation evaluation) {
        Section section = new Section(overallCard(evaluation), dimensionCard(evaluation));
        section.addClassName("da-summary-grid");
        return section;
    }

    private static Div overallCard(DesignEvaluation evaluation) {
        Span label = new Span("Overall");
        label.addClassName("da-meta");

        Span value = new Span(String.valueOf(evaluation.overallScore()));
        value.addClassName("da-score__value");
        Span total = new Span("/ 10");
        total.addClassName("da-score__total");

        Div score = new Div(value, total);
        score.addClassName("da-score");

        Div card = new Div(label, score);
        card.addClassNames("da-card", "da-card--flat");
        return card;
    }

    private static Div dimensionCard(DesignEvaluation evaluation) {
        Span label = new Span("By rubric dimension");
        label.addClassName("da-meta");

        Div card = new Div(label);
        card.addClassNames("da-card", "da-card--flat");
        evaluation.dimensions().forEach(dimension -> card.add(dimensionRow(dimension)));
        return card;
    }

    private static Div dimensionRow(DimensionScore dimension) {
        Span name = new Span(dimension.dimension());
        name.getElement().setAttribute("title", dimension.comment());

        Div fill = new Div();
        fill.addClassName("da-bar__fill");
        fill.getStyle().set("--da-bar-fill", dimension.score() * 10 + "%");
        // A weak score must not read as a strong one at a glance.
        fill.getStyle().set("background", colourFor(dimension.score()));

        Div bar = new Div(fill);
        bar.addClassName("da-bar");
        bar.getElement().setAttribute("aria-hidden", "true");

        Span score = new Span(String.valueOf(dimension.score()));
        score.addClassName("da-dimension__score");

        Div row = new Div(name, bar, score);
        row.addClassName("da-dimension");
        return row;
    }

    private static String colourFor(int score) {
        if (score >= 7) {
            return "var(--da-brand)";
        }
        return score >= 5 ? "var(--da-neutral)" : "var(--da-warn)";
    }

    private static Section findings(DesignEvaluation evaluation) {
        Section section = new Section(
                findingCard("What you did well", "var(--da-brand)", evaluation.strengths()),
                findingCard("What's wrong or risky", "var(--da-danger)", evaluation.issues()),
                findingCard("What's missing", "var(--da-warn)", evaluation.missing()),
                findingCard("Follow-up questions", "var(--da-text-chip)", evaluation.followUpQuestions()));
        section.addClassName("da-findings");
        return section;
    }

    private static Div findingCard(String title, String colour, List<String> items) {
        Span dot = new Span();
        dot.addClassName("da-finding__dot");
        dot.getStyle().set("--da-dot", colour);
        dot.getElement().setAttribute("aria-hidden", "true");

        H2 heading = new H2(title);
        heading.addClassName("da-card__heading");

        Div headingRow = new Div(dot, heading);
        headingRow.addClassName("da-card__heading-row");

        UnorderedList list = new UnorderedList();
        list.addClassName("da-finding__list");
        items.forEach(item -> list.add(new ListItem(item)));

        Div card = new Div(headingRow, list);
        card.addClassNames("da-card", "da-card--flat");
        return card;
    }

    private Component submission() {
        Element details = new Element("details");
        details.getClassList().add("da-details");

        Element summary = new Element("summary");
        summary.setText("Your submission");
        details.appendChild(summary);

        Paragraph content = new Paragraph(state.submittedDesign()
                + (state.submittedDiagram().isBlank() ? "" : "\n\n" + state.submittedDiagram()));
        content.addClassName("da-details__content");
        details.appendChild(content.getElement());

        Div wrapper = new Div();
        wrapper.getElement().appendChild(details);
        return wrapper;
    }

    private Div provenance() {
        Span text = new Span(state.provenance().orElse(""));
        text.addClassName("da-provenance__text");

        Anchor revise = new Anchor("design/" + state.problemSlug(), "Revise and resubmit");
        revise.addClassNames("da-button", "da-button--secondary");

        Anchor another = new Anchor("/", "Try another problem");
        another.addClassNames("da-button", "da-button--primary");

        Div actions = new Div(revise, another);
        actions.addClassName("da-provenance__actions");

        Div row = new Div(text, actions);
        row.addClassName("da-provenance");
        return row;
    }
}
