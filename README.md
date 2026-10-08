# Migration Decision Framework: Selenium → Playwright (Java)

The practical companion to the "would I migrate 120 tests" post.
Three steps, in order, matching the post.

The three tools are **pure JDK** (Java 17+), so there is nothing to install.

## 1. Collect evidence from the existing suite

```bash
java src/main/java/migration/GenerateFixtureReports.java   # demo data: 5 runs
java src/main/java/migration/AnalyzeSuite.java
```

```
Total tests:                    120
Flaky across runs:              32  (27%)
Avg execution time:             18.5 min  (across 5 runs)
Failing tests, waits/locators:  14
Failing tests, real defects:    18
```

To use your real suite, skip the generator and drop your own JUnit XML
exports into `reports/` named `run_1.xml`, `run_2.xml`, and so on
(Surefire, TestNG and TestSigma can all export this format).
Run the suite several times first, because flakiness only shows up
across repeated runs.

## 2. Run a POC on 10-15 representative scenarios

`src/test/java/poc/` holds the same login scenario in both frameworks:

- `LoginSeleniumTest.java` (fixed sleep + brittle locators)
- `LoginPlaywrightTest.java` (auto-wait + role locators + trace.zip)

These are templates: replace `BASE_URL` and the locators with a real page
of your app, then extend to login, forms, tables, API-dependent flows,
file handling and multi-page journeys. They need Maven and Chrome:

```bash
mvn test
```

## 3. Score the POC, get a verdict

Edit `scorecard.csv` with your actual POC results (1-5 per criterion):

```bash
java src/main/java/migration/DecisionScorecard.java
```

Output is **KEEP / REFACTOR / MIGRATE**, plus a risk-first migration
order when the verdict is MIGRATE.

## Why this matters

The decision should survive a POC, metrics, CI execution and
maintenance, not a tool comparison article.
