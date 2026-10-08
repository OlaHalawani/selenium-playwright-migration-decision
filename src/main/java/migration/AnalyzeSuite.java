package migration;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/**
 * Evidence step: "I'd run the existing suite first and collect evidence."
 *
 * Reads every reports/run_*.xml (repeated runs of the same suite, standard
 * JUnit XML) and prints the numbers you'd put in front of a migration
 * decision, instead of a gut feeling about the tool.
 *
 * Run:  java src/main/java/migration/AnalyzeSuite.java
 */
public class AnalyzeSuite {

    static final Pattern LOCATOR = Pattern.compile(
        "NoSuchElementException|TimeoutException|ElementClickIntercepted", Pattern.CASE_INSENSITIVE);

    record Attempt(boolean passed, String message) {}

    public static void main(String[] args) throws Exception {
        List<Path> runs;
        try (Stream<Path> s = Files.list(Path.of("reports"))) {
            runs = s.filter(p -> p.getFileName().toString().matches("run_\\d+\\.xml")).sorted().toList();
        } catch (IOException e) {
            runs = List.of();
        }
        if (runs.isEmpty()) {
            System.err.println("No reports found. Run GenerateFixtureReports.java first.");
            System.exit(1);
        }

        Map<String, List<Attempt>> results = new LinkedHashMap<>();
        double totalRuntime = 0;

        for (Path run : runs) {
            Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(run.toFile()).getDocumentElement();
            totalRuntime += Double.parseDouble(root.getAttribute("time"));
            NodeList cases = root.getElementsByTagName("testcase");
            for (int i = 0; i < cases.getLength(); i++) {
                Element c = (Element) cases.item(i);
                NodeList failures = c.getElementsByTagName("failure");
                Attempt a = failures.getLength() > 0
                    ? new Attempt(false, ((Element) failures.item(0)).getAttribute("message"))
                    : new Attempt(true, null);
                results.computeIfAbsent(c.getAttribute("name"), k -> new ArrayList<>()).add(a);
            }
        }

        List<String> flaky = new ArrayList<>();
        Set<String> locatorTests = new HashSet<>(), defectTests = new HashSet<>();
        Map<String, Double> failRate = new HashMap<>();

        for (var e : results.entrySet()) {
            List<Attempt> attempts = e.getValue();
            long fails = attempts.stream().filter(a -> !a.passed()).count();
            if (fails > 0 && fails < attempts.size()) {      // passed AND failed at least once
                flaky.add(e.getKey());
                failRate.put(e.getKey(), (double) fails / attempts.size());
            }
            for (Attempt a : attempts) {
                if (!a.passed()) {
                    (LOCATOR.matcher(a.message()).find() ? locatorTests : defectTests).add(e.getKey());
                }
            }
        }

        int total = results.size();
        double avgMinutes = totalRuntime / runs.size() / 60;

        System.out.println("=".repeat(60));
        System.out.println(" SUITE HEALTH REPORT: evidence before any tool decision");
        System.out.println("=".repeat(60));
        System.out.printf("  Total tests:                    %d%n", total);
        System.out.printf("  Flaky across runs:              %d  (%.0f%%)%n", flaky.size(), 100.0 * flaky.size() / total);
        System.out.printf("  Avg execution time:             %.1f min  (across %d runs)%n", avgMinutes, runs.size());
        System.out.printf("  Failing tests, waits/locators:  %d%n", locatorTests.size());
        System.out.printf("  Failing tests, real defects:    %d%n", defectTests.size());
        System.out.println("-".repeat(60));
        System.out.println("  Top 5 flakiest tests:");
        failRate.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
            .limit(5)
            .forEach(e -> System.out.printf("    - %-28s failed %.0f%% of runs%n", e.getKey(), e.getValue() * 100));
        System.out.println("=".repeat(60));
        System.out.println("This is the input to DecisionScorecard, not a Selenium-vs-");
        System.out.println("Playwright opinion. The numbers decide whether a POC is worth it.");
    }
}
