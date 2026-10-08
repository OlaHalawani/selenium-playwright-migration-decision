package poc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Same scenario, Selenium side of the POC.
 *
 * This is the pattern behind the "waits/locators" flaky tests: a fixed
 * sleep and a brittle CSS selector that break the moment the UI shifts
 * by one pixel or one network tick.
 *
 * Swap BASE_URL and the locators for a real page in your app.
 */
class LoginSeleniumTest {

    static final String BASE_URL = "https://app.example.com/login";

    @Test
    void loginSucceedsAndShowsDashboard() throws InterruptedException {
        WebDriver driver = new ChromeDriver();
        try {
            driver.get(BASE_URL);

            // Flaky pattern #1: fixed sleep, a guess about how long the page needs
            Thread.sleep(2000);

            // Flaky pattern #2: positional locators, break on any DOM change
            driver.findElement(By.cssSelector("div.form > input:nth-child(1)")).sendKeys("ola@example.com");
            driver.findElement(By.cssSelector("div.form > input:nth-child(2)")).sendKeys("password123");
            driver.findElement(By.cssSelector("button.submit-btn")).click();

            // An explicit wait helps, but only for this one element
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(By.id("dashboard-welcome")));

            assertTrue(driver.findElement(By.id("dashboard-welcome")).getText().contains("Welcome"));
        } finally {
            driver.quit();
        }
    }
}
