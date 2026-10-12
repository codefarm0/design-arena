package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import in.codefarm.designarena.problem.Difficulty;
import in.codefarm.designarena.problem.ProblemAdmin;
import in.codefarm.designarena.problem.ProblemDraft;
import in.codefarm.designarena.problem.ProblemNotFoundException;
import in.codefarm.designarena.problem.RubricDimensionDraft;
import in.codefarm.designarena.problem.RubricDraft;

import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Editing one problem: its content, and the rubric a design for it is judged against.
 *
 * <p>Ordered lists of plain strings — requirements, scale hints, must-haves — are edited
 * as one line per item. That is the whole list visible at once, reordering is moving a
 * line, and it needs no add, remove or drag machinery. The moment these grow structure
 * beyond text they deserve a real editor; today they do not have any.
 */
@Route(value = "admin/problem/:slug", layout = MainLayout.class)
@PageTitle("Edit problem · Design Arena")
@RolesAllowed("ADMIN")
public final class AdminProblemView extends Div implements BeforeEnterObserver, HasHeaderActions {

    private final transient ProblemAdmin admin;

    private final TextField title = new TextField("Title");
    private final Select<Difficulty> difficulty = new Select<>();
    private final TextField blurb = new TextField("Catalogue blurb");
    private final TextArea statement = new TextArea("Statement");
    private final TextArea functional = new TextArea("Functional requirements");
    private final TextArea nonFunctional = new TextArea("Non-functional requirements");
    private final TextArea scaleHints = new TextArea("Scale hints");
    private final Checkbox published = new Checkbox("Published in the catalogue");
    private final Div rubricFields = new Div();
    private final Span message = new Span();

    private String slug;

    public AdminProblemView(ProblemAdmin admin) {
        this.admin = admin;
        addClassNames("da-view", "da-view--results");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        slug = event.getRouteParameters().get("slug").orElse("");
        try {
            populate(admin.draftOf(slug), admin.rubricDraftOf(slug));
        } catch (ProblemNotFoundException e) {
            event.forwardTo(AdminProblemsView.class);
        }
    }

    @Override
    public Component headerActions() {
        Anchor back = new Anchor("admin", "All problems");
        back.addClassName("da-nav__link");
        return back;
    }

    private void populate(ProblemDraft draft, RubricDraft rubric) {
        removeAll();

        title.setValue(draft.title());
        difficulty.setLabel("Difficulty");
        difficulty.setItems(Difficulty.values());
        difficulty.setValue(draft.difficulty());
        blurb.setValue(draft.blurb());
        statement.setValue(draft.statementMarkdown());
        functional.setValue(String.join("\n", draft.functionalRequirements()));
        nonFunctional.setValue(String.join("\n", draft.nonFunctionalRequirements()));
        scaleHints.setValue(String.join("\n", draft.scaleHints()));
        published.setValue(draft.active());

        List.of(title, blurb, statement, functional, nonFunctional, scaleHints).forEach(field -> field.setWidthFull());
        List.of(functional, nonFunctional, scaleHints).forEach(field -> field.setHelperText("One per line, in order."));
        statement.setHelperText("Markdown. Shown beside the editor.");

        H1 heading = new H1(draft.title());
        heading.addClassName("da-page-heading__title");
        Span slugLabel = new Span(slug);
        slugLabel.addClassName("da-meta");
        Div header = new Div(slugLabel, heading);
        header.addClassName("da-page-heading");

        message.addClassName("da-caption");
        message.getElement().setAttribute("role", "status");

        NativeButton save = new NativeButton("Save problem", event -> save());
        save.addClassNames("da-button", "da-button--primary");

        Div form = new Div(
                title, difficulty, blurb, statement, functional, nonFunctional, scaleHints, published, save, message);
        form.addClassName("da-form");
        Section card = new Section(form);
        card.addClassNames("da-card", "da-card--flat");

        add(header, card, rubricCard(rubric));
    }

    private Section rubricCard(RubricDraft rubric) {
        H2 heading = new H2("Rubric");
        heading.addClassName("da-card__heading");
        Span note = new Span("Dimension names are sent to the model verbatim and appear on the review page. "
                + "Editing them does not change reviews already produced.");
        note.addClassName("da-caption");

        rubricFields.removeAll();
        rubricFields.addClassName("da-form");
        rubric.dimensions().forEach(dimension -> rubricFields.add(dimensionFields(dimension)));

        NativeButton save = new NativeButton("Save rubric", event -> saveRubric());
        save.addClassNames("da-button", "da-button--primary");

        Section card = new Section(heading, note, rubricFields, save);
        card.addClassNames("da-card", "da-card--flat");
        return card;
    }

    /** One dimension's fields, kept together so the save can read them back in order. */
    private static Div dimensionFields(RubricDimensionDraft dimension) {
        TextField name = new TextField("Dimension");
        name.setValue(dimension.name());
        name.setWidthFull();

        TextField weight = new TextField("Weight");
        weight.setValue(String.valueOf(dimension.weight()));

        TextArea description = new TextArea("What it judges");
        description.setValue(dimension.description());
        description.setWidthFull();

        TextArea mustHaves = new TextArea("Must-haves");
        mustHaves.setValue(String.join("\n", dimension.mustHaves()));
        mustHaves.setHelperText("One per line.");
        mustHaves.setWidthFull();

        Div group = new Div(name, weight, description, mustHaves);
        group.addClassNames("da-card", "da-card--flat");
        return group;
    }

    private void save() {
        admin.save(new ProblemDraft(
                slug,
                title.getValue(),
                difficulty.getValue(),
                blurb.getValue(),
                statement.getValue(),
                lines(functional.getValue()),
                lines(nonFunctional.getValue()),
                lines(scaleHints.getValue()),
                published.getValue()));
        message.setText("Saved.");
    }

    private void saveRubric() {
        List<RubricDimensionDraft> dimensions = new ArrayList<>();
        for (Component group : rubricFields.getChildren().toList()) {
            List<Component> fields = group.getChildren().toList();
            TextField name = (TextField) fields.get(0);
            TextField weight = (TextField) fields.get(1);
            TextArea description = (TextArea) fields.get(2);
            TextArea mustHaves = (TextArea) fields.get(3);
            if (name.getValue().isBlank()) {
                continue;
            }
            dimensions.add(new RubricDimensionDraft(
                    name.getValue().strip(),
                    description.getValue().strip(),
                    parseWeight(weight.getValue()),
                    lines(mustHaves.getValue())));
        }
        admin.saveRubric(slug, new RubricDraft(dimensions));
        message.setText("Rubric saved.");
    }

    private static int parseWeight(String value) {
        try {
            return Math.max(1, Integer.parseInt(value.strip()));
        } catch (NumberFormatException e) {
            // A weight that will not parse is an editing slip, not a reason to lose the
            // whole save; the smallest sensible weight is a safe reading of the intent.
            return 1;
        }
    }

    /** Splits a textarea into an ordered list, ignoring blank lines. */
    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\n"))
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .toList();
    }
}
