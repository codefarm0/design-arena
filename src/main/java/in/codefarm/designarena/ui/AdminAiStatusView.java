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
import in.codefarm.designarena.ai.AiReviewer;
import in.codefarm.designarena.ai.AiStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;

import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Which model is actually answering, and how it has been doing.
 *
 * <p>The first question about a strange review is always "what produced it?", and the
 * answer should not be "read the YAML and hope that is what deployed". Every value here
 * is read from the beans that are wired in and the meters they have recorded.
 */
@Route(value = "admin/ai", layout = MainLayout.class)
@PageTitle("AI status · Design Arena")
@RolesAllowed("ADMIN")
public final class AdminAiStatusView extends Div implements HasHeaderActions {

    public AdminAiStatusView(AiReviewer reviewer, MeterRegistry meters) {
        addClassNames("da-view", "da-view--results");

        AiStatus status = reviewer.status();

        H1 title = new H1("AI status");
        title.addClassName("da-page-heading__title");
        Paragraph lead = new Paragraph("Read from the running application, not from configuration files.");
        lead.addClassName("da-hero__lead");
        Div heading = new Div(title, lead);
        heading.addClassName("da-page-heading");

        add(heading, providerCard(status), promptCard(status), callsCard(meters), reviewsCard(meters));
    }

    @Override
    public Component headerActions() {
        Anchor back = new Anchor("admin", "All problems");
        back.addClassName("da-nav__link");
        return back;
    }

    private static Section providerCard(AiStatus status) {
        Section card = card("Provider");
        card.add(
                row("Provider", status.provider()),
                row("Model", status.model()),
                row("Model calls", status.enabled() ? "enabled" : "disabled"),
                row("Retries on an unusable reply", String.valueOf(status.maxRetries())));
        return card;
    }

    private static Section promptCard(AiStatus status) {
        Section card = card("Prompts");
        status.promptVersions().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> card.add(row(entry.getKey(), entry.getValue())));
        return card;
    }

    /** Latency and outcome of every model call, from the timer the AI layer records. */
    private static Section callsCard(MeterRegistry meters) {
        Section card = card("Model calls");
        List<Timer> timers =
                meters.find("designarena.ai.call").timers().stream().toList();
        if (timers.isEmpty()) {
            card.add(empty("No model calls recorded since this application started."));
            return card;
        }
        timers.forEach(timer -> card.add(row(
                "%s · %s"
                        .formatted(timer.getId().getTag("prompt"), timer.getId().getTag("outcome")),
                "%d calls, mean %.1fs".formatted(timer.count(), timer.mean(TimeUnit.SECONDS)))));
        return card;
    }

    private static Section reviewsCard(MeterRegistry meters) {
        Section card = card("Usage since startup");
        card.add(
                row("Quizzes submitted", count(meters, "designarena.quiz.submitted", null)),
                row("Reviews completed", count(meters, "designarena.review.completed", "DONE")),
                row("Reviews failed", count(meters, "designarena.review.completed", "FAILED")),
                row("Reviews blocked by the plan limit", count(meters, "designarena.review.blocked", null)));
        return card;
    }

    /** A counter that has never been incremented does not exist yet, which reads as zero. */
    private static String count(MeterRegistry meters, String name, String status) {
        Search search = meters.find(name);
        if (status != null) {
            search = search.tag("status", status);
        }
        long total =
                (long) search.counters().stream().mapToDouble(Counter::count).sum();
        return String.valueOf(total);
    }

    private static Section card(String title) {
        H2 heading = new H2(title);
        heading.addClassName("da-card__heading");
        Section card = new Section(heading);
        card.addClassNames("da-card", "da-card--flat");
        return card;
    }

    private static Div row(String label, String value) {
        Span name = new Span(label);
        Span detail = new Span(value);
        detail.addClassName("da-history__outcome");
        Div row = new Div(name, detail);
        row.addClassName("da-status-row");
        return row;
    }

    private static Span empty(String text) {
        Span span = new Span(text);
        span.addClassName("da-caption");
        return span;
    }
}
