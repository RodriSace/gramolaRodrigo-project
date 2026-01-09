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
import com.example.gramolaRodrigo.repositories.EmailVerificationTokenRepository;
import com.example.gramolaRodrigo.repositories.QueuedSongRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionRepository;

import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SuccessfulPaymentTest {

    @Autowired private QueuedSongRepository queuedSongRepository;
    @Autowired private BarRepository barRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private EmailVerificationTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        tokenRepository.deleteAll();
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
        
        // --- BLOQUEO DEFINITIVO DE POPUPS DE GOOGLE ---
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);
        
        options.addArguments("--disable-features=PasswordLeakDetection");
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--incognito");

        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDown() { if (driver != null) driver.quit(); }

    @Test
    public void testSuccessfulPaymentAndDatabase() {
        driver.get("http://localhost:8080/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("verse@ejemplo.com");
        driver.findElement(By.id("password")).sendKeys("1234");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[placeholder*='Busca']")));
        searchInput.sendKeys("Daft Punk", Keys.ENTER);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".song-card")));
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".song-card button"))).click();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#payment-element iframe")));
        driver.switchTo().frame(driver.findElement(By.cssSelector("#payment-element iframe")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("cardnumber"))).sendKeys("4242424242424242");
        driver.findElement(By.name("exp-date")).sendKeys("1230"); // Fecha lejana
        driver.findElement(By.name("cvc")).sendKeys("123");
        driver.switchTo().defaultContent();

        try { Thread.sleep(1000); } catch (Exception e) {} // Pausa técnica para Stripe
        driver.findElement(By.cssSelector(".btn-pay")).click();

        // ESPERA DINÁMICA HASTA QUE EL CONTADOR SEA > 0
        boolean saved = false;
        for(int i=0; i<10; i++) {
            if(queuedSongRepository.count() > 0) { saved = true; break; }
            try { Thread.sleep(1000); } catch (Exception e) {}
        }
        Assertions.assertTrue(saved, "La canción no se guardó en la BD tras el pago");
    }
}