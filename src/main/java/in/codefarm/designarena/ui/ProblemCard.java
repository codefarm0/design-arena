package in.codefarm.designarena.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import in.codefarm.designarena.problem.ProblemSummary;

/**
 * One problem on the catalogue: difficulty, question count, title, blurb and the two ways
 * to practise it.
 */
final class ProblemCard extends Article {

    ProblemCard(ProblemSummary problem, long questionCount) {
        addClassName("da-card");

        Span difficulty = new Span(problem.difficulty().name());
        difficulty.addClassName("da-chip");

        Span questions = new Span(questionLabel(questionCount));
        questions.addClassName("da-meta");

        Div header = new Div(difficulty, questions);
        header.addClassName("da-card__header");

        H3 title = new H3(problem.title());
        title.addClassName("da-card__title");

        Paragraph blurb = new Paragraph(problem.blurb());
        blurb.addClassName("da-card__blurb");

        add(header, title, blurb, actions(problem.slug()));
    }

    private static String questionLabel(long questionCount) {
        return questionCount == 1 ? "1 question" : questionCount + " questions";
    }

    /** The two practice modes, both live. */
    private static Div actions(String slug) {
        Anchor quiz = new Anchor("quiz/" + slug, "Take quiz");
        quiz.addClassNames("da-button", "da-button--primary");

        Anchor design = new Anchor("design/" + slug, "Write a design");
        design.addClassNames("da-button", "da-button--secondary");

        Div actions = new Div(quiz, design);
        actions.addClassName("da-card__actions");
        return actions;
    }
}
