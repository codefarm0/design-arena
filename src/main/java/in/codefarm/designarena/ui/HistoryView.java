package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemSummary;
import in.codefarm.designarena.quiz.QuizHistoryEntry;
import in.codefarm.designarena.quiz.QuizService;
import in.codefarm.designarena.review.Allowance;
import in.codefarm.designarena.review.ReviewHistoryEntry;
import in.codefarm.designarena.review.ReviewService;
import in.codefarm.designarena.user.Account;
import in.codefarm.designarena.user.Accounts;

import jakarta.annotation.security.PermitAll;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything this person has done, and what their plan has left.
 *
 * <p>No mockup exists for this screen either, so it reuses the card, chip and meta
 * patterns from the approved ones.
 */
@Route(value = "history", layout = MainLayout.class)
@PageTitle("History · Design Arena")
@PermitAll
public final class HistoryView extends Div implements HasHeaderActions {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy");

    /** Problems by id, so a history row can show a title and link to the right slug. */
    private final transient Map<Long, ProblemSummary> problemsById = new HashMap<>();

    public HistoryView(Accounts accounts, QuizService quizzes, ReviewService reviews, ProblemCatalogue problems) {
        addClassNames("da-view", "da-view--results");

        Account account = accounts.currentOrFail();
        problems.activeProblems().forEach(problem -> problemsById.put(problem.id(), problem));

        add(heading(account), allowanceCard(reviews.allowanceFor(account)));
        add(reviewList(reviews.historyFor(account)), quizList(quizzes.historyFor(account)));
    }

    @Override
    public Component headerActions() {
        Anchor practice = new Anchor("/", "Practice");
        practice.addClassName("da-nav__link");
        return practice;
    }

    private Div heading(Account account) {
        Span who = new Span(account.displayName() + " · " + account.plan().name() + " plan");
        who.addClassName("da-meta");

        H1 title = new H1("Your history");
        title.addClassName("da-page-heading__title");

        Div heading = new Div(who, title);
        heading.addClassName("da-page-heading");
        return heading;
    }

    /**
     * What is left this month.
     *
     * <p>Shown here and on the editor, because somebody about to spend their last review
     * should know it before they write a design, not after they submit one.
     */
    private Section allowanceCard(Allowance allowance) {
        Span label = new Span("Design reviews this month");
        label.addClassName("da-meta");

        Span value = new Span(
                allowance.capped()
                        ? "%d of %d used"
                                .formatted(allowance.used(), allowance.limit().orElseThrow())
                        : "%d used".formatted(allowance.used()));
        value.addClassName("da-card__heading");

        Section card = new Section(label, value);
        card.addClassNames("da-card", "da-card--flat");

        allowance
                .resetsOn()
                .ifPresentOrElse(
                        date -> {
                            Span note = new Span("Resets on " + WHEN.format(date));
                            note.addClassName("da-caption");
                            card.add(note);
                        },
                        () -> {
                            Span note = new Span("Your plan has no monthly cap.");
                            note.addClassName("da-caption");
                            card.add(note);
                        });
        return card;
    }

    private Section reviewList(List<ReviewHistoryEntry> entries) {
        Section section = sectionWith("Design reviews", entries.size());
        if (entries.isEmpty()) {
            section.add(empty("No design reviews yet. Pick a problem and write one."));
            return section;
        }
        entries.forEach(entry -> section.add(row(
                titleOf(entry.problemId()),
                WHEN.format(entry.submittedAt().atZone(ZoneOffset.UTC)),
                entry.statusLabel(),
                "review/" + entry.submissionId())));
        return section;
    }

    private Section quizList(List<QuizHistoryEntry> entries) {
        Section section = sectionWith("Quizzes", entries.size());
        if (entries.isEmpty()) {
            section.add(empty("No finished quizzes yet."));
            return section;
        }
        entries.forEach(entry -> section.add(row(
                titleOf(entry.problemId()),
                WHEN.format(entry.submittedAt().atZone(ZoneOffset.UTC)),
                "%d / %d".formatted(entry.score(), entry.maxScore()),
                "quiz/%s/results/%d".formatted(slugOf(entry.problemId()), entry.attemptId()))));
        return section;
    }

    private static Section sectionWith(String title, int count) {
        H2 heading = new H2(title);
        heading.addClassName("da-section__title");
        Span total = new Span(count == 1 ? "1 entry" : count + " entries");
        total.addClassName("da-meta");

        Div header = new Div(heading, total);
        header.addClassName("da-section__header");

        Section section = new Section(header);
        section.addClassName("da-section");
        return section;
    }

    private static Div row(String title, String when, String outcome, String href) {
        Span name = new Span(title);
        name.addClassName("da-result__question");

        Span date = new Span(when);
        date.addClassName("da-meta");

        Span result = new Span(outcome);
        result.addClassName("da-history__outcome");

        Anchor open = new Anchor(href, "Open");
        open.addClassNames("da-button", "da-button--secondary");

        Div entry = new Div(new Div(name, date), result, open);
        entry.addClassName("da-history__row");
        return entry;
    }

    private static Paragraph empty(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.addClassNames("da-prose", "da-caption");
        return paragraph;
    }

    /** A problem that has since been retired still has history pointing at it. */
    private String titleOf(long problemId) {
        ProblemSummary problem = problemsById.get(problemId);
        return problem == null ? "A problem that is no longer listed" : problem.title();
    }

    private String slugOf(long problemId) {
        ProblemSummary problem = problemsById.get(problemId);
        return problem == null ? "" : problem.slug();
    }
}
