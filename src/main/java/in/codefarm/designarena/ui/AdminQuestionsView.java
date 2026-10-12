package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import in.codefarm.designarena.problem.ProblemCatalogue;
import in.codefarm.designarena.problem.ProblemDetail;
import in.codefarm.designarena.problem.ProblemNotFoundException;
import in.codefarm.designarena.quiz.InvalidQuestionException;
import in.codefarm.designarena.quiz.OptionDraft;
import in.codefarm.designarena.quiz.QuestionDraft;
import in.codefarm.designarena.quiz.QuestionType;
import in.codefarm.designarena.quiz.QuizAdmin;

import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;

/**
 * Editing the questions for one problem.
 *
 * <p>Each question is a card with its options beneath it, saved on its own. Saving one
 * question at a time rather than the whole set means a validation failure on question
 * seven cannot discard the edits made to question two.
 */
@Route(value = "admin/problem/:slug/questions", layout = MainLayout.class)
@PageTitle("Edit questions · Design Arena")
@RolesAllowed("ADMIN")
public final class AdminQuestionsView extends Div implements BeforeEnterObserver, HasHeaderActions {

    /** Four options is what the seeded questions use and what the design is drawn for. */
    private static final int OPTIONS_PER_QUESTION = 4;

    private final transient QuizAdmin quizAdmin;
    private final transient ProblemCatalogue problems;

    private transient ProblemDetail problem;

    public AdminQuestionsView(QuizAdmin quizAdmin, ProblemCatalogue problems) {
        this.quizAdmin = quizAdmin;
        this.problems = problems;
        addClassNames("da-view", "da-view--results");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String slug = event.getRouteParameters().get("slug").orElse("");
        try {
            problem = problems.detailBySlug(slug);
        } catch (ProblemNotFoundException e) {
            event.forwardTo(AdminProblemsView.class);
            return;
        }
        render();
    }

    @Override
    public Component headerActions() {
        Anchor back = new Anchor("admin", "All problems");
        back.addClassName("da-nav__link");
        return back;
    }

    private void render() {
        removeAll();

        Span where = new Span(problem.title());
        where.addClassName("da-meta");
        H1 title = new H1("Questions");
        title.addClassName("da-page-heading__title");
        Div heading = new Div(where, title);
        heading.addClassName("da-page-heading");
        add(heading);

        List<QuestionDraft> drafts = quizAdmin.draftsFor(problem.id());
        if (drafts.isEmpty()) {
            Paragraph empty = new Paragraph("No questions yet. Add the first one below.");
            empty.addClassNames("da-prose", "da-caption");
            add(empty);
        }
        drafts.forEach(draft -> add(questionCard(draft)));
        add(questionCard(blankQuestion(drafts.size() + 1)));
    }

    private static QuestionDraft blankQuestion(int position) {
        List<OptionDraft> options = new ArrayList<>();
        for (int i = 0; i < OPTIONS_PER_QUESTION; i++) {
            options.add(new OptionDraft("", false, ""));
        }
        return new QuestionDraft(null, position, "", QuestionType.SINGLE, "", options);
    }

    private Section questionCard(QuestionDraft draft) {
        TextArea text = new TextArea("Question " + draft.position());
        text.setValue(draft.text());
        text.setWidthFull();

        Select<QuestionType> type = new Select<>();
        type.setLabel("Type");
        type.setItems(QuestionType.values());
        type.setValue(draft.type());

        TextField category = new TextField("Category");
        category.setValue(draft.category());
        category.setHelperText("Scored separately on the results page, for example CONSISTENCY.");

        Div optionFields = new Div();
        optionFields.addClassName("da-form");
        draft.options().forEach(option -> optionFields.add(optionFields(option)));

        Span message = new Span();
        message.addClassName("da-caption");
        message.getElement().setAttribute("role", "status");

        NativeButton save = new NativeButton(
                draft.id() == null ? "Add question" : "Save question",
                event -> save(draft, text, type, category, optionFields, message));
        save.addClassNames("da-button", "da-button--primary");

        Div form = new Div(text, type, category, optionFields, save, message);
        form.addClassName("da-form");

        Section card = new Section(form);
        card.addClassNames("da-card", "da-card--flat");
        return card;
    }

    private static Div optionFields(OptionDraft option) {
        TextField text = new TextField("Option");
        text.setValue(option.text());
        text.setWidthFull();

        Checkbox correct = new Checkbox("Correct");
        correct.setValue(option.correct());

        TextArea explanation = new TextArea("Why");
        explanation.setValue(option.explanation());
        explanation.setWidthFull();
        explanation.setHelperText("Required for wrong options too: this is what the results page shows.");

        Div group = new Div(text, correct, explanation);
        group.addClassNames("da-card", "da-card--flat");
        return group;
    }

    private void save(
            QuestionDraft original,
            TextArea text,
            Select<QuestionType> type,
            TextField category,
            Div optionFields,
            Span message) {
        List<OptionDraft> options = new ArrayList<>();
        for (Component group : optionFields.getChildren().toList()) {
            List<Component> fields = group.getChildren().toList();
            TextField optionText = (TextField) fields.get(0);
            Checkbox correct = (Checkbox) fields.get(1);
            TextArea explanation = (TextArea) fields.get(2);
            if (optionText.getValue().isBlank() && explanation.getValue().isBlank()) {
                continue;
            }
            options.add(new OptionDraft(optionText.getValue(), correct.getValue(), explanation.getValue()));
        }

        try {
            quizAdmin.save(
                    problem.id(),
                    new QuestionDraft(
                            original.id(),
                            original.position(),
                            text.getValue(),
                            type.getValue(),
                            category.getValue(),
                            options));
            render();
        } catch (InvalidQuestionException e) {
            // Content mistakes are told to the editor in words, on the question they were
            // editing, rather than thrown away as a stack trace.
            message.setText(e.getMessage());
            message.getElement().setAttribute("role", "alert");
        }
    }
}
