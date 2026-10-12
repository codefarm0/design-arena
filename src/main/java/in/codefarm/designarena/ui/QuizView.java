package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import in.codefarm.designarena.problem.ProblemNotFoundException;
import in.codefarm.designarena.quiz.QuestionType;
import in.codefarm.designarena.quiz.QuizQuestion;
import in.codefarm.designarena.quiz.QuizService;
import in.codefarm.designarena.quiz.QuizSession;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The quiz itself: one question per screen, with a progress bar.
 *
 * <p>Built to {@code docs/ui/ui/Quiz.html}.
 *
 * <p>Answers are sent to the server as the user moves between questions rather than all
 * at once at the end, so closing the tab on question seven does not discard the first six.
 */
@Route(value = "quiz/:slug", layout = MainLayout.class)
@PageTitle("Quiz · Design Arena")
@AnonymousAllowed
public final class QuizView extends Div implements BeforeEnterObserver, HasHeaderActions {

    private final QuizService quiz;

    /** What the user has selected so far, so moving back and forth keeps the answers. */
    private final Map<Long, Set<Long>> selections = new HashMap<>();

    private QuizSession session;
    private int index;

    public QuizView(QuizService quiz) {
        this.quiz = quiz;
        addClassNames("da-view", "da-view--narrow");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String slug = event.getRouteParameters().get("slug").orElse("");
        try {
            session = quiz.start(slug);
        } catch (ProblemNotFoundException e) {
            // A hand-edited or stale URL, not a fault: send them back to the catalogue.
            event.forwardTo(ProblemCatalogueView.class);
            return;
        }
        index = 0;
        render();
    }

    @Override
    public Component headerActions() {
        Anchor exit = new Anchor("/", "Exit quiz");
        exit.addClassName("da-nav__link");
        return exit;
    }

    private void render() {
        removeAll();
        QuizQuestion question = session.questions().get(index);
        add(progress(), questionCard(question), actions());
    }

    private Div progress() {
        int total = session.questions().size();
        int current = index + 1;

        Span where = new Span(session.problemTitle() + " · Quiz");
        Span count = new Span("Question %d of %d".formatted(current, total));
        count.addClassName("da-progress__count");

        Div labels = new Div(where, count);
        labels.addClassName("da-progress__labels");

        Div fill = new Div();
        fill.addClassName("da-progress__fill");
        fill.getStyle().set("--da-bar-fill", Math.round(current * 100f / total) + "%");

        Div track = new Div(fill);
        track.addClassName("da-progress__track");
        track.getElement().setAttribute("role", "progressbar");
        track.getElement().setAttribute("aria-label", "Quiz progress");
        track.getElement().setAttribute("aria-valuenow", String.valueOf(current));
        track.getElement().setAttribute("aria-valuemin", "0");
        track.getElement().setAttribute("aria-valuemax", String.valueOf(total));

        Div progress = new Div(labels, track);
        progress.addClassName("da-progress");
        return progress;
    }

    private Section questionCard(QuizQuestion question) {
        Div chips = new Div(
                new Chip(question.category()),
                new Chip(question.type() == QuestionType.SINGLE ? "SINGLE CHOICE" : "MULTI CHOICE"));
        chips.addClassName("da-chip-row");

        H1 text = new H1(question.text());
        text.addClassName("da-question__text");

        QuestionOptions options = new QuestionOptions(
                question,
                selections.getOrDefault(question.id(), Set.of()),
                selected -> recordAnswer(question, selected));

        Section card = new Section(chips, text, options);
        card.addClassName("da-question");
        return card;
    }

    private void recordAnswer(QuizQuestion question, Set<Long> selected) {
        selections.put(question.id(), new LinkedHashSet<>(selected));
        quiz.answer(session.attemptId(), question.id(), selected);
    }

    private Div actions() {
        boolean lastQuestion = index == session.questions().size() - 1;

        NativeButton previous = new NativeButton("← Previous", event -> move(-1));
        previous.addClassNames("da-button", "da-button--secondary");
        previous.setEnabled(index > 0);

        NativeButton skip = new NativeButton("Skip", event -> move(1));
        skip.addClassNames("da-button", "da-button--ghost");
        skip.setVisible(!lastQuestion);

        NativeButton next = new NativeButton(lastQuestion ? "See results →" : "Next →", event -> advance(lastQuestion));
        next.addClassNames("da-button", "da-button--primary");

        Div right = new Div(skip, next);
        right.addClassName("da-quiz-actions__right");

        Div actions = new Div(previous, right);
        actions.addClassName("da-quiz-actions");
        return actions;
    }

    private void advance(boolean lastQuestion) {
        if (lastQuestion) {
            quiz.submit(session.attemptId());
            RouteParameters route = new RouteParameters(
                    Map.of("slug", session.problemSlug(), "attemptId", String.valueOf(session.attemptId())));
            getUI().ifPresent(ui -> ui.navigate(QuizResultsView.class, route));
            return;
        }
        move(1);
    }

    private void move(int delta) {
        index = Math.clamp(index + delta, 0, session.questions().size() - 1);
        render();
    }
}
