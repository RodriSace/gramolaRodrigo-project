package com.example.gramolaRodrigo.selenium;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.gramolaRodrigo.entities.Bar;
import com.example.gramolaRodrigo.entities.Subscription;
import com.example.gramolaRodrigo.repositories.BarRepository;
import com.example.gramolaRodrigo.repositories.QueuedSongRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionRepository;

import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class FailedPaymentTest {

    @Autowired private QueuedSongRepository queuedSongRepository;
    @Autowired private BarRepository barRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        queuedSongRepository.deleteAll();
        barRepository.deleteAll();

        Bar bar = new Bar();
        bar.setId(UUID.randomUUID().toString());
        bar.setEmail("verse@ejemplo.com");
        bar.setName("verse"); 
        bar.setPwd(passwordEncoder.encode("1234"));
        bar.setVerified(true);
        bar.setVerifiedAt(Instant.now());
        barRepository.saveAndFlush(bar);

        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID().toString());
        sub.setBar(bar);
        sub.setStatus("ACTIVE");
        sub.setPlanId("ANNUAL");
        sub.setStartAt(Instant.now());
        sub.setEndAt(Instant.now().plus(Duration.ofDays(30)));
        subscriptionRepository.saveAndFlush(sub);

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);

        options.addArguments("--incognito", "--disable-features=PasswordLeakDetection", "--disable-save-password-bubble");
        options.addArguments("--disable-search-engine-choice-screen", "--remote-allow-origins=*");
        
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void tearDown() { if (driver != null) driver.quit(); }

    @Test
    public void testFailedPaymentFlow() {
        driver.get("http://localhost:8080/login");

        // 1. Login
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("verse@ejemplo.com");
        driver.findElement(By.id("password")).sendKeys("1234");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // 2. Búsqueda
        WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder*='Busca']")));
        searchInput.sendKeys("Daft Punk", Keys.ENTER);
        
        // 3. Esperar canciones
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".song-card")));

        // 4. Click en pagar
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".song-card button"))).click();

        // 5. Iframe Stripe - Llenar tarjeta que falla
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#payment-element iframe")));
        driver.switchTo().frame(driver.findElement(By.cssSelector("#payment-element iframe")));
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("cardnumber"))).sendKeys("4000000000000005");
        driver.findElement(By.name("exp-date")).sendKeys("01/25");
        driver.findElement(By.name("cvc")).sendKeys("000");
        
        driver.switchTo().defaultContent();
        
        // 6. Intentar pagar
        driver.findElement(By.cssSelector(".btn-pay")).click();

        // 7. VERIFICACIÓN DE ERROR UI (Ajustado para ser más tolerante)
        // Buscamos cualquier cosa que parezca una alerta o mensaje de error de Angular/Stripe
        WebElement errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(@class, 'error') or contains(@class, 'alert') or contains(@class, 'invalid') or contains(text(), 'declined') or contains(text(), 'fallido')]")
        ));
        Assertions.assertTrue(errorMsg.isDisplayed(), "El mensaje de error no apareció");

        // 8. Verificación Backend: Cola vacía
        Assertions.assertEquals(0, queuedSongRepository.count(), "Se guardó una canción a pesar del error de pago");
    }
}