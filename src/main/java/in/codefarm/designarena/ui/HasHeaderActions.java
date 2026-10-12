package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;

/**
 * Implemented by a view that needs something other than the default navigation on the
 * right of the header.
 *
 * <p>The mockups use the same header everywhere with only that part changing: the full
 * navigation on the catalogue, "Exit quiz" during a quiz, "Back to problems" on a results
 * page. This keeps one header component rather than one per screen.
 */
interface HasHeaderActions {

    /** What to show on the right of the header for this screen. */
    Component headerActions();
}
