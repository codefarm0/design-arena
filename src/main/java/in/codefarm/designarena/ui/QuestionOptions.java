package in.codefarm.designarena.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import in.codefarm.designarena.quiz.QuestionType;
import in.codefarm.designarena.quiz.QuizOption;
import in.codefarm.designarena.quiz.QuizQuestion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The answers for one question, as a labelled group of radio buttons or checkboxes.
 *
 * <p>Built from native inputs inside a {@code <fieldset>} rather than from Vaadin's
 * radio-button group. The design makes each option a bordered card that changes colour
 * when selected, which means styling the element that wraps the input — and that element
 * lives inside the Vaadin component's shadow DOM. Native inputs keep the structure the
 * mockup specifies and the grouping semantics assistive technology expects, with no
 * shadow-part workarounds.
 */
final class QuestionOptions extends Div {

    private final List<Entry> entries = new ArrayList<>();
    private final QuestionType type;
    private final Consumer<Set<Long>> onChange;

    QuestionOptions(QuizQuestion question, Set<Long> selected, Consumer<Set<Long>> onChange) {
        this.type = question.type();
        this.onChange = onChange;

        Element fieldset = new Element("fieldset");
        fieldset.getClassList().add("da-options");

        Element legend = new Element("legend");
        legend.getClassList().add("da-visually-hidden");
        legend.setText(type == QuestionType.SINGLE ? "Choose one answer" : "Choose every answer that applies");
        fieldset.appendChild(legend);

        for (QuizOption option : question.options()) {
            Entry entry = new Entry(question, option, selected.contains(option.id()));
            entries.add(entry);
            fieldset.appendChild(entry.label.getElement());
        }
        getElement().appendChild(fieldset);
    }

    private void selectionChanged(Entry changed, boolean nowSelected) {
        if (type == QuestionType.SINGLE && nowSelected) {
            entries.stream().filter(entry -> entry != changed).forEach(entry -> entry.setSelected(false));
        }
        changed.setSelected(nowSelected);
        onChange.accept(selection());
    }

    private Set<Long> selection() {
        Set<Long> selected = new LinkedHashSet<>();
        entries.stream().filter(entry -> entry.selected).forEach(entry -> selected.add(entry.optionId));
        return selected;
    }

    /** One option: the card, its input, and whether it is currently chosen. */
    private final class Entry {

        private final long optionId;
        private final NativeLabel label;
        private final Input input;
        private boolean selected;

        private Entry(QuizQuestion question, QuizOption option, boolean initiallySelected) {
            this.optionId = option.id();
            this.selected = initiallySelected;

            this.input = new Input();
            input.setType(question.type() == QuestionType.SINGLE ? "radio" : "checkbox");
            input.getElement().setAttribute("name", "question-" + question.id());
            input.addClassName("da-option__input");
            input.getElement().setProperty("checked", initiallySelected);
            // The browser owns a native input's checked state and never syncs it back, so
            // the new value has to travel with the event rather than be read from the
            // server-side element, which would still hold the value we last set.
            input.getElement()
                    .addEventListener(
                            "change",
                            event -> selectionChanged(
                                    this,
                                    event.getEventData().path("element.checked").asBoolean(false)))
                    .addEventData("element.checked");

            Span key = new Span(option.key());
            key.addClassName("da-option__key");
            Span text = new Span(option.text());

            Div body = new Div(key, text);
            body.addClassName("da-option__body");

            this.label = new NativeLabel();
            label.addClassName("da-option");
            label.add(input, body);
            applySelectedClass();
        }

        private void setSelected(boolean value) {
            this.selected = value;
            input.getElement().setProperty("checked", value);
            applySelectedClass();
        }

        private void applySelectedClass() {
            label.getElement().getClassList().set("da-option--selected", selected);
        }
    }
}
