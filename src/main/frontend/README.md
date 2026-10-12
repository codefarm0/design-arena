# Frontend sources

Vaadin owns this directory as its frontend root, which means two kinds of file end up
here. The rule for telling them apart:

| Path | Who writes it | Committed |
|---|---|---|
| `styles/` | us | yes |
| `generated/` | the Vaadin build, on every run | no — git-ignored |

`generated/` cannot be moved out of `src/`: Vaadin always writes it inside its frontend
root, and that root has to contain our stylesheets for `@CssImport` to resolve them.

The npm project (`package.json`, `node_modules`, `tsconfig.json`, `types.d.ts`) is
generated too, and stays at the project root for the same kind of reason — Vite resolves
the generated `index.html` relative to it, so relocating it breaks the production build.
Everything generated is git-ignored. Vaadin's own build directory is redirected into
`build/` by the `vaadin { }` block in `build.gradle.kts`; one code path ignores that and
still writes a `target/` folder, so `./gradlew clean` deletes it explicitly.

Never edit anything in `generated/`: it is recreated from scratch by the next build.
