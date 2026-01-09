package com.example.gramolaRodrigo.selenium;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
        options.addArguments("--incognito");
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--remote-allow-origins=*");
        
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void tearDown() { if (driver != null) driver.quit(); }

    @Test
    public void testFailedPaymentFlow() {
        driver.get("http://localhost:8080/login");

        // Login
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("verse@ejemplo.com");
        driver.findElement(By.id("password")).sendKeys("1234");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Búsqueda
        WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[placeholder*='Busca']")));
        searchInput.sendKeys("Daft Punk", Keys.ENTER);
        
        // Selección canción
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".song-card")));
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".song-card button"))).click();

        // Iframe Stripe
        WebElement stripeIframe = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#payment-element iframe")));
        driver.switchTo().frame(stripeIframe);
        
        // Usamos una tarjeta que Stripe reconoce como "Válida pero denegada" para que permita clicar en Pagar
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("cardnumber"))).sendKeys("4000000000000005");
        driver.findElement(By.name("exp-date")).sendKeys("1230");
        driver.findElement(By.name("cvc")).sendKeys("123");
        
        driver.switchTo().defaultContent();
        
        // Esperamos a que el botón de pagar esté listo
        try { Thread.sleep(1500); } catch (Exception e) {}
        WebElement btnPay = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-pay")));
        
        // Forzamos el click con JS por si hay algún overlay invisible de Stripe
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnPay);

        // BUSQUEDA DEL ERROR (Híbrida):
        boolean errorFound = false;
        
        // 1. Intentamos buscar el texto en la página principal (donde Angular muestra errores de Stripe)
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(), 'no es válido') or contains(text(), 'incorrecto') or contains(@class, 'error-msg')]")));
            errorFound = true;
        } catch (Exception e) {
            // 2. Si no está fuera, volvemos a entrar al Iframe para ver si Stripe lo puso allí
            driver.switchTo().frame(stripeIframe);
            WebElement innerError = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(., 'válido') or @role='alert']")));
            errorFound = innerError.isDisplayed();
            driver.switchTo().defaultContent();
        }
        
        Assertions.assertTrue(errorFound, "No se encontró ningún mensaje de error en la página ni en el iframe");
        Assertions.assertEquals(0, queuedSongRepository.count(), "Se guardó la canción a pesar del pago fallido");
    }
}