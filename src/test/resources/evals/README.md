# Eval set

Sample submissions used to check that the reviewer actually discriminates between a good
design and a bad one. Three per problem: `strong`, `average`, `weak`.

These are **not** unit-test fixtures. They are run against the real configured model by
`DesignReviewEvalTest`, which is skipped unless you ask for it:

```bash
./gradlew test -Devals=true --tests '*DesignReviewEvalTest*'
```

The assertion is deliberately coarse — strong must outscore weak by a clear margin —
because the exact numbers a model produces will drift between models and prompt versions,
while the ordering should not. A prompt change that inverts the ordering is a regression
no unit test would catch.
