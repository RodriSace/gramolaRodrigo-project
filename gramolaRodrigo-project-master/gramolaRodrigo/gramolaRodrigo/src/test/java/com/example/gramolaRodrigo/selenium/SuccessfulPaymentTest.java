package com.example.gramolaRodrigo.selenium;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
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
    @Autowired private PasswordEncoder passwordEncoder; // <--- CRÍTICO: Para sincronizar la clave

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        // 1. Limpieza total de seguridad
        subscriptionRepository.deleteAll();
        tokenRepository.deleteAll();
        queuedSongRepository.deleteAll();
        barRepository.deleteAll();

        // 2. Crear Bar usando el encoder real del sistema
        Bar bar = new Bar();
        bar.setId(UUID.randomUUID().toString());
        bar.setEmail("verse@ejemplo.com");
        bar.setName("verse");
        bar.setPwd(passwordEncoder.encode("1234")); // <--- Genera el hash dinámicamente
        bar.setVerified(true);
        bar.setVerifiedAt(Instant.now());
        barRepository.saveAndFlush(bar);

        // 3. Inyectar suscripción activa para saltar pantalla /subscribe
        Subscription sub = new Subscription();
        sub.setId(UUID.randomUUID().toString());
        sub.setBar(bar);
        sub.setStatus("ACTIVE");
        sub.setPlanId("ANNUAL");
        sub.setStartAt(Instant.now());
        sub.setEndAt(Instant.now().plus(Duration.ofDays(30)));
        subscriptionRepository.saveAndFlush(sub);

        // 4. Configurar Selenium
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-search-engine-choice-screen", "--remote-allow-origins=*");
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDown() { if (driver != null) driver.quit(); }

    @Test
    public void testSuccessfulPaymentAndDatabase() {
        driver.get("http://localhost:8080/login");

        // Login
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("verse@ejemplo.com");
        driver.findElement(By.id("password")).sendKeys("1234");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // CONTROL DE ALERT: Si aparece el alert de credenciales, el test nos avisará
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            String texto = alert.getText();
            alert.accept();
            Assertions.fail("Fallo de login detectado: " + texto);
        } catch (TimeoutException e) {
            // No hay alert, login exitoso
        }

        // Búsqueda y Pago
        WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[placeholder*='Busca']")));
        searchInput.sendKeys("Daft Punk", Keys.ENTER);

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".song-card button"))).click();
        
        // Iframe Stripe
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#payment-element iframe")));
        driver.switchTo().frame(driver.findElement(By.cssSelector("#payment-element iframe")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("cardnumber"))).sendKeys("4242424242424242");
        driver.findElement(By.name("exp-date")).sendKeys("0128");
        driver.findElement(By.name("cvc")).sendKeys("123");
        driver.switchTo().defaultContent();

        driver.findElement(By.cssSelector(".btn-pay")).click();

        // Verificación BD (Requisito 1 del examen)
        try { Thread.sleep(3000); } catch (Exception e) {}
        Assertions.assertTrue(queuedSongRepository.count() > 0, "La canción no se guardó en la BD");
    }
}