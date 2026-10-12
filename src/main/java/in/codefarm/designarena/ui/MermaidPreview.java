package in.codefarm.designarena.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * The live diagram preview beside the Mermaid editor.
 *
 * <p>Mermaid renders in the browser, so this is a thin Java wrapper around a custom
 * element. Vaadin installs the npm package and bundles the module as part of the normal
 * build, which keeps the one piece of JavaScript in this project inside the same build
 * and the same version control as everything else.
 */
@Tag("mermaid-preview")
@NpmPackage(value = "mermaid", version = "11.17.2")
@JsModule("./mermaid-preview.js")
final class MermaidPreview extends Component {

    /** Replaces the diagram source. Invalid source shows a hint, never an error. */
    void setCode(String code) {
        getElement().setProperty("code", code == null ? "" : code);
    }
}
