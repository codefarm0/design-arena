package in.codefarm.designarena.problem;

/** Thrown when a new problem reuses an existing slug, which URLs already depend on. */
public class SlugAlreadyUsedException extends RuntimeException {

    public SlugAlreadyUsedException(String slug) {
        super("A problem already uses the slug " + slug);
    }
}
