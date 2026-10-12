package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import in.codefarm.designarena.quiz.CategoryScore;
import in.codefarm.designarena.quiz.QuestionResult;
import in.codefarm.designarena.quiz.QuizNotFoundException;
import in.codefarm.designarena.quiz.QuizResults;
import in.codefarm.designarena.quiz.QuizService;

import java.util.List;

/**
 * What the answers added up to: the score, the per-category breakdown, the AI narrative
 * when there is one, and an explanation for every question.
 *
 * <p>Built to {@code docs/ui/ui/QuizResults.html}.
 *
 * <p>The one thing to notice in this class is how little depends on the model: the
 * narrative is a single optional card, and everything else renders from the score.
 */
@Route(value = "quiz/:slug/results/:attemptId", layout = MainLayout.class)
@PageTitle("Quiz results · Design Arena")
@AnonymousAllowed
public final class QuizResultsView extends Div implements BeforeEnterObserver, HasHeaderActions {

    private static final int INITIALLY_VISIBLE_RESULTS = 3;

    /**
     * How often the page asks whether the narrative has arrived, and for how long.
     *
     * <p>Polling rather than server push: it needs no extra configuration, costs one tiny
     * request every couple of seconds for at most a minute, and the page is correct even
     * if the poll never fires.
     */
    private static final int POLL_INTERVAL_MS = 2_000;

    private static final int POLL_ATTEMPTS = 45;

    private final QuizService quiz;
    private QuizResults results;
    private int pollsRemaining = POLL_ATTEMPTS;
    private Registration pollRegistration;

    public QuizResultsView(QuizService quiz) {
        this.quiz = quiz;
        addClassNames("da-view", "da-view--results");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        long attemptId = event.getRouteParameters().getLong("attemptId").orElse(-1L);
        try {
            results = quiz.results(attemptId);
        } catch (QuizNotFoundException e) {
            event.forwardTo(ProblemCatalogueView.class);
            return;
        }
        render();
        if (results.summary().isEmpty()) {
            awaitSummary();
        }
    }

    /**
     * Waits for the narrative being written on a background thread, and gives up after a
     * minute and a half. Giving up is a normal outcome: the card then says the summary is
     * unavailable, which is exactly what it says when the model fails outright.
     */
    private void awaitSummary() {
        UI ui = UI.getCurrent();
        ui.setPollInterval(POLL_INTERVAL_MS);
        pollRegistration = ui.addPollListener(event -> {
            QuizResults latest = quiz.results(results.attemptId());
            if (latest.summary().isPresent() || --pollsRemaining <= 0) {
                results = latest;
                stopPolling();
                render();
            }
        });
        // Leaving the page must stop the polling with it: the listener lives on the UI,
        // which outlives this view, so without this it would keep querying for a page
        // nobody is looking at.
        addDetachListener(event -> stopPolling());
    }

    private void stopPolling() {
        if (pollRegistration != null) {
            pollRegistration.remove();
            pollRegistration = null;
        }
        getUI().ifPresent(ui -> ui.setPollInterval(-1));
    }

    @Override
    public Component headerActions() {
        Anchor back = new Anchor("/", "Back to problems");
        back.addClassName("da-nav__link");
        return back;
    }

    private void render() {
        removeAll();
        add(heading(), scoreSection(), summaryCard(), questionList(), nextStepBanner());
    }

    private Div heading() {
        Span where = new Span(results.problemTitle() + " · Quiz results");
        where.addClassName("da-meta");

        H1 title = new H1("Nice work. Here's how your decisions stack up.");
        title.addClassName("da-page-heading__title");

        Div heading = new Div(where, title);
        heading.addClassName("da-page-heading");
        return heading;
    }

    private Section scoreSection() {
        Section section = new Section(overallScoreCard(), categoryCard());
        section.addClassName("da-summary-grid");
        return section;
    }

    private Div overallScoreCard() {
        Span label = new Span("Overall score");
        label.addClassName("da-meta");

        Span value = new Span(String.valueOf(results.score()));
        value.addClassName("da-score__value");
        Span total = new Span("/ " + results.maxScore());
        total.addClassName("da-score__total");

        Div score = new Div(value, total);
        score.addClassName("da-score");

        Span note = new Span("Scored by the answer key, not the AI.");
        note.addClassName("da-meta");

        Div card = new Div(label, score, note);
        card.addClassNames("da-card", "da-card--flat");
        return card;
    }

    private Div categoryCard() {
        Span label = new Span("By category");
        label.addClassName("da-meta");

        Div card = new Div(label);
        card.addClassNames("da-card", "da-card--flat");
        for (CategoryScore category : results.categories()) {
            card.add(new ScoreBar(category.category(), category.score(), category.maxScore(), category.percentage()));
        }
        return card;
    }

    /**
     * The only part of this page a language model produced.
     *
     * <p>When the model did not answer, the card says so plainly and the rest of the page
     * is unchanged. That is the whole fallback: no spinner, no retry, no apology.
     */
    private Section summaryCard() {
        H2 heading = new H2("Your design in summary");
        heading.addClassName("da-card__heading");

        Div headingRow = new Div(heading, Chip.brand("AI SUMMARY"));
        headingRow.addClassName("da-card__heading-row");

        Section card = new Section(headingRow);
        card.addClassNames("da-card", "da-card--flat");

        results.summary()
                .ifPresentOrElse(
                        text -> {
                            Paragraph prose = new Paragraph(text);
                            prose.addClassName("da-prose");
                            card.add(prose);
                        },
                        () -> card.add(waitingOrUnavailable()));
        return card;
    }

    /**
     * The two no-summary states. While the background thread is still working the card
     * says so; once we have stopped waiting it says the summary is unavailable. Both make
     * clear that nothing else on the page depends on it.
     */
    private Div waitingOrUnavailable() {
        boolean stillWaiting = pollsRemaining > 0;

        Paragraph prose = new Paragraph(
                stillWaiting ? "Writing your summary…" : "Summary unavailable — the model did not answer this time.");
        prose.addClassName("da-prose");

        Span note = new Span("Your score and the explanations below are ready either way.");
        note.addClassName("da-caption");

        Div state = new Div(prose, note);
        // Announced to assistive technology when it changes, without stealing focus.
        state.getElement().setAttribute("role", "status");
        return state;
    }

    private Section questionList() {
        List<QuestionResult> questions = results.questions();

        H2 heading = new H2("Question by question");
        heading.addClassName("da-section__title");
        Span shown = new Span(
                "Showing %d of %d".formatted(Math.min(INITIALLY_VISIBLE_RESULTS, questions.size()), questions.size()));
        shown.addClassName("da-meta");

        Div header = new Div(heading, shown);
        header.addClassName("da-section__header");

        Div list = new Div();
        list.addClassName("da-results-list");
        questions.stream().limit(INITIALLY_VISIBLE_RESULTS).forEach(result -> list.add(resultCard(result)));

        Section section = new Section(header, list);
        section.addClassName("da-section");

        if (questions.size() > INITIALLY_VISIBLE_RESULTS) {
            NativeButton showAll = new NativeButton("Show all " + questions.size(), event -> {
                list.removeAll();
                questions.forEach(result -> list.add(resultCard(result)));
                shown.setText("Showing all %d".formatted(questions.size()));
                event.getSource().setVisible(false);
            });
            showAll.addClassNames("da-button", "da-button--secondary");
            section.add(showAll);
        }
        return section;
    }

    private Article resultCard(QuestionResult result) {
        Article card = new Article();
        card.addClassName("da-result");

        // The icon carries meaning, so it is labelled rather than hidden, and the wording
        // below repeats it: correctness is never signalled by colour alone.
        Image icon = new Image(
                result.correct() ? "images/correct.svg" : "images/incorrect.svg",
                result.correct() ? "Correct" : "Incorrect");
        icon.addClassName("da-result__icon");

        Span meta = new Span("Q%d · %s".formatted(result.position(), result.category()));
        meta.addClassName("da-result__meta");

        Span question = new Span(result.text());
        question.addClassName("da-result__question");

        Div answers = new Div(answerLine(
                "Your answer: ", result.answered() ? String.join(", ", result.yourAnswers()) : "Not answered"));
        answers.addClassName("da-result__answers");
        if (!result.correct()) {
            answers.add(answerLine("Correct answer: ", String.join(", ", result.correctAnswers())));
        }

        Paragraph why = new Paragraph(result.explanation());
        why.addClassName("da-result__why");

        Div body = new Div(meta, question, answers, why);
        body.addClassName("da-result__body");

        card.add(icon, body);
        return card;
    }

    private static Span answerLine(String label, String value) {
        Span prefix = new Span(label);
        prefix.addClassName("da-result__label");
        return new Span(prefix, new Span(value));
    }

    private Section nextStepBanner() {
        Span title = new Span("Ready for the real test?");
        title.addClassName("da-banner__title");
        Span detail = new Span("Write the full design and get it reviewed against the rubric.");
        detail.addClassName("da-banner__detail");

        Div text = new Div(title, detail);
        text.addClassName("da-banner__text");

        Anchor write = new Anchor("design/" + results.problemSlug(), "Write a design →");
        write.addClassNames("da-button", "da-button--inverse");

        Section banner = new Section(text, write);
        banner.addClassNames("da-banner", "da-banner--dark");
        return banner;
    }
}
