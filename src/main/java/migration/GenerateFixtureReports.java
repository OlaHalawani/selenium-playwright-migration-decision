package migration;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Generates synthetic JUnit XML reports for 5 repeated runs of a 120-test
 * Selenium suite, reproducing the evidence profile from the post:
 *   - 120 tests total
 *   - 32 flaky across the 5 runs (pass sometimes, fail sometimes)
 *   - 14 of those fail because of waits/locators, not product defects
 *   - ~18 minutes total execution time per run
 *
 * Run:  java src/main/java/migration/GenerateFixtureReports.java
 */
public class GenerateFixtureReports {

    static final int TOTAL_TESTS = 120, FLAKY = 32, LOCATOR_FLAKY = 14, RUNS = 5;
    static final double TARGET_SECONDS = 18 * 60;

    static final String[] SCENARIOS = {
        "login", "logout", "forgot_password", "signup_form", "profile_update",
        "search_results", "filter_table", "sort_table", "pagination",
        "file_upload", "file_download", "checkout_flow", "add_to_cart",
        "remove_from_cart", "api_dependent_fetch", "multi_page_wizard",
        "modal_confirm", "modal_cancel", "nav_menu", "settings_toggle"
    };
    static final String[] LOCATOR_ERRORS = {
        "NoSuchElementException: Unable to locate element: css selector",
        "TimeoutException: Timed out waiting for element to be clickable"
    };
    static final String[] DEFECT_ERRORS = {
        "AssertionError: expected balance 500 but got 480",
        "AssertionError: expected status 'APPROVED' but got 'PENDING'"
    };

    public static void main(String[] args) throws IOException {
        Random rnd = new Random(42);

        List<String> names = new ArrayList<>();
        for (int i = 0; i < TOTAL_TESTS; i++) {
            names.add(String.format("%s_%03d", SCENARIOS[rnd.nextInt(SCENARIOS.length)], i));
        }

        List<String> shuffled = new ArrayList<>(names);
        Collections.shuffle(shuffled, rnd);
        Set<String> flaky = new HashSet<>(shuffled.subList(0, FLAKY));
        List<String> flakyList = new ArrayList<>(flaky);
        Collections.sort(flakyList);
        Collections.shuffle(flakyList, rnd);
        Set<String> locatorFlaky = new HashSet<>(flakyList.subList(0, LOCATOR_FLAKY));

        // Base duration per test, scaled so one run is ~18 minutes
        Map<String, Double> base = new HashMap<>();
        double sum = 0;
        for (String n : names) { double d = 5 + rnd.nextDouble() * 7; base.put(n, d); sum += d; }
        double scale = TARGET_SECONDS / sum;

        // Per-test outcome across runs. A flaky test is guaranteed to
        // pass at least once AND fail at least once.
        Map<String, boolean[]> failsInRun = new HashMap<>();
        for (String n : names) {
            boolean[] f = new boolean[RUNS];
            if (flaky.contains(n)) {
                boolean any = false, all = true;
                for (int r = 0; r < RUNS; r++) { f[r] = rnd.nextDouble() < 0.45; any |= f[r]; all &= f[r]; }
                if (!any) f[rnd.nextInt(RUNS)] = true;
                if (all)  f[rnd.nextInt(RUNS)] = false;
            }
            failsInRun.put(n, f);
        }

        Files.createDirectories(Path.of("reports"));
        for (int run = 0; run < RUNS; run++) {
            StringBuilder body = new StringBuilder();
            double total = 0;
            for (String n : names) {
                double dur = base.get(n) * scale * (0.9 + rnd.nextDouble() * 0.25);
                total += dur;
                body.append(String.format("  <testcase classname=\"regression\" name=\"%s\" time=\"%.2f\"", n, dur));
                if (failsInRun.get(n)[run]) {
                    String[] pool = locatorFlaky.contains(n) ? LOCATOR_ERRORS : DEFECT_ERRORS;
                    String msg = pool[rnd.nextInt(pool.length)];
                    body.append(">\n    <failure message=\"").append(msg).append("\"/>\n  </testcase>\n");
                } else {
                    body.append("/>\n");
                }
            }
            String xml = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testsuite name=\"SeleniumRegressionSuite\" tests=\"%d\" time=\"%.2f\">\n%s</testsuite>\n",
                TOTAL_TESTS, total, body);
            Files.writeString(Path.of("reports", "run_" + (run + 1) + ".xml"), xml);
        }
        System.out.println("Wrote " + RUNS + " reports to ./reports/ (" + TOTAL_TESTS + " tests each)");
    }
}
