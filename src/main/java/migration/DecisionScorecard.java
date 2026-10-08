package migration;

import java.nio.file.*;
import java.util.*;

/**
 * Decision step: turns POC results into KEEP / REFACTOR / MIGRATE
 * instead of a tool preference.
 *
 * Fill in scorecard.csv with your POC's actual scores (1-5 per criterion,
 * for the same 10-15 scenarios run on both frameworks), then:
 *   java src/main/java/migration/DecisionScorecard.java
 */
public class DecisionScorecard {

    record Criterion(String name, double weight, int selenium, int playwright) {}

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of("scorecard.csv"));
        List<Criterion> criteria = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {      // skip header
            if (line.isBlank()) continue;
            String[] p = line.split(",");
            criteria.add(new Criterion(p[0].trim(), Double.parseDouble(p[1]),
                Integer.parseInt(p[2].trim()), Integer.parseInt(p[3].trim())));
        }

        double sel = criteria.stream().mapToDouble(c -> c.weight() * c.selenium()).sum();
        double pw  = criteria.stream().mapToDouble(c -> c.weight() * c.playwright()).sum();
        double gap = pw - sel;

        System.out.println("=".repeat(64));
        System.out.println(" MIGRATION SCORECARD (weighted, 1-5 per criterion)");
        System.out.println("=".repeat(64));
        System.out.printf("%-42s%10s%12s%n", "Criterion", "Selenium", "Playwright");
        for (Criterion c : criteria) {
            System.out.printf("%-42s%10d%12d   (w=%.2f)%n", c.name(), c.selenium(), c.playwright(), c.weight());
        }
        System.out.println("-".repeat(64));
        System.out.printf("%-42s%10.2f%12.2f%n", "WEIGHTED TOTAL", sel, pw);
        System.out.println("=".repeat(64));

        // The gap has to clear the migration cost, not just "be higher"
        String verdict, why;
        if (gap < 0.4) {
            verdict = "KEEP";
            why = "Selenium is stable and maintainable enough. Migration gives little measurable value.";
        } else if (gap < 0.9) {
            verdict = "REFACTOR";
            why = "The gap is architecture, not the tool. Fix locator strategy, waits, and structure first.";
        } else {
            verdict = "MIGRATE";
            why = "The POC shows enough measurable improvement to justify the migration cost.";
        }

        System.out.println("\nVERDICT: " + verdict);
        System.out.println("WHY:     " + why);

        if (verdict.equals("MIGRATE")) {
            System.out.println("""

                MIGRATION ORDER (risk-first, both suites run in parallel until cutover):
                  1. Critical journeys   (payment, auth, KYC)
                  2. Regression suite    (highest historical defect density)
                  3. Remaining scenarios (lowest risk, migrate opportunistically)
                """);
        }
    }
}
