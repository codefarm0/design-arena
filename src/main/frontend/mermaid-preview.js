import mermaid from 'mermaid';

/**
 * Renders Mermaid source as a diagram, for the live preview beside the editor.
 *
 * Mermaid runs in the browser, so this is the one piece of the application written in
 * JavaScript: there is no server-side Mermaid renderer worth the dependency, and sending
 * the diagram to the server on every keystroke to get an image back would be worse in
 * every way.
 *
 * Invalid source is normal while someone is typing, so a parse error is shown as a quiet
 * message rather than thrown.
 */
mermaid.initialize({
  startOnLoad: false,
  securityLevel: 'strict',
  theme: 'base',
  themeVariables: {
    fontFamily: "'IBM Plex Sans', system-ui, sans-serif",
    primaryColor: '#E3F1EE',
    primaryTextColor: '#14161B',
    primaryBorderColor: '#0E6B5C',
    lineColor: '#555B66',
  },
});

let renderCount = 0;

class MermaidPreview extends HTMLElement {
  set code(value) {
    this._code = value ?? '';
    this._render();
  }

  get code() {
    return this._code ?? '';
  }

  connectedCallback() {
    this._render();
  }

  async _render() {
    const source = this.code.trim();
    if (!source) {
      this.innerHTML = '<p class="da-preview__hint">Your diagram appears here as you type.</p>';
      return;
    }
    try {
      const { svg } = await mermaid.render(`mermaid-preview-${renderCount++}`, source);
      this.innerHTML = svg;
    } catch (error) {
      // Half-typed Mermaid is not an error worth shouting about.
      this.innerHTML = '<p class="da-preview__hint">Not valid Mermaid yet.</p>';
    }
  }
}

customElements.define('mermaid-preview', MermaidPreview);
