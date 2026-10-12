package in.codefarm.designarena.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemSummary;
import in.codefarm.designarena.quiz.QuizCatalogue;

import java.util.List;
import java.util.Map;

/**
 * The landing screen: the hero, then every active problem as a card.
 *
 * <p>Built to {@code docs/ui/ui/Main.html}. All styling comes from the theme's tokens, so
 * there is no colour or spacing value in this class.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Problem catalogue · Design Arena")
@AnonymousAllowed
public final class ProblemCatalogueView extends Div {

    /**
     * Two services, because the problem and the number of questions are owned by two
     * different features. Composing them here keeps the dependency pointing one way: the
     * view knows both features, and neither feature knows the other.
     */
    public ProblemCatalogueView(ProblemCatalogue problems, QuizCatalogue quizzes) {
        addClassName("da-view");

        List<ProblemSummary> catalogue = problems.activeProblems();
        Map<Long, Long> questionCounts = quizzes.questionCountsByProblemId();

        add(hero(), problems(catalogue, questionCounts), learnMore());
    }

    private static Section hero() {
        Span eyebrow = new Span("SYSTEM DESIGN PRACTICE");
        eyebrow.addClassName("da-eyebrow");

        H1 title = new H1("Practise system design. Get staff-level feedback.");
        title.addClassName("da-hero__title");

        Paragraph lead = new Paragraph("Pick a problem. Test your decisions with a quick quiz, or write a full design "
                + "and get it reviewed against a staff-engineer rubric.");
        lead.addClassName("da-hero__lead");

        Section hero = new Section(eyebrow, title, lead);
        hero.addClassName("da-hero");
        return hero;
    }

    private static Section problems(List<ProblemSummary> catalogue, Map<Long, Long> questionCounts) {
        H2 heading = new H2("Problems");
        heading.addClassName("da-section__title");

        Span count = new Span(catalogue.size() == 1 ? "1 problem" : catalogue.size() + " problems");
        count.addClassName("da-meta");

        Div header = new Div(heading, count);
        header.addClassName("da-section__header");

        Div grid = new Div();
        grid.addClassName("da-card-grid");
        catalogue.forEach(problem -> grid.add(new ProblemCard(problem, questionCounts.getOrDefault(problem.id(), 0L))));

        Section section = new Section(header, grid);
        section.addClassName("da-section");
        return section;
    }

    private static Section learnMore() {
        Span title = new Span("New to a topic?");
        title.addClassName("da-banner__title");

        Span detail = new Span("Read the concepts first, then come back to practise.");
        detail.addClassName("da-banner__detail");

        Div text = new Div(title, detail);
        text.addClassName("da-banner__text");

        Anchor link = new Anchor("https://codefarm.in", "Learn on codefarm.in →");
        link.addClassName("da-banner__link");

        Section banner = new Section(text, link);
        banner.addClassName("da-banner");
        return banner;
    }
}
