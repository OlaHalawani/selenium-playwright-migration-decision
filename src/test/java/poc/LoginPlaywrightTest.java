package poc;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.nio.file.Paths;
import org.junit.jupiter.api.*;

/**
 * Same scenario, Playwright side of the POC.
 *
 * Auto-waiting removes flaky pattern #1 entirely. Role/label locators
 * survive most class-name and layout changes, removing most of pattern #2.
 * The trace.zip written at the end (DOM snapshots + network + console)
 * is the "Debugging" line in the scorecard.
 *
 * Swap BASE_URL and the locators for a real page in your app.
 */
class LoginPlaywrightTest {

    static final String BASE_URL = "https://app.example.com/login";

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        context.tracing().start(new Tracing.StartOptions()
            .setScreenshots(true).setSnapshots(true).setSources(true));
        page = context.newPage();
    }

    @AfterEach
    void saveTraceAndClose() {
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/trace.zip")));
        context.close();
    }

    @Test
    void loginSucceedsAndShowsDashboard() {
        page.navigate(BASE_URL);

        // No sleep: Playwright waits for each element to be actionable
        page.getByLabel("Email").fill("ola@example.com");
        page.getByLabel("Password").fill("password123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();

        // Auto-retrying assertion: polls until true or timeout, no manual wait
        assertThat(page.getByTestId("dashboard-welcome")).containsText("Welcome");
    }
}
