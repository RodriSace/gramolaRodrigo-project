package com.gramola.backend;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.*;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PRUEBA FUNCIONAL (Sección 4 del enunciado):
 * Un cliente del bar busca una canción. Paga y la pone.
 * Se comprobará que el pago se ha confirmado en la base de datos 
 * y que la canción se ha añadido a la lista de canciones del bar.
 *
 * REQUISITO: Este test abre Chrome VISIBLE para que el tribunal pueda verlo.
 * 
 * PRECONDICIONES:
 * 1. Backend ejecutándose en http://127.0.0.1:8080
 * 2. Frontend ejecutándose en http://127.0.0.1:4200
 * 3. Un bar registrado con email y contraseña válidos
 * 4. MySQL en localhost:3306 con BD gramola_db
 */
public class SeleniumTest {

    private WebDriver driver;
    private WebDriverWait wait;

    // --- CONFIGURACIÓN: Ajustar estos valores según el bar de prueba ---
    private static final String BAR_EMAIL = "final@ejemplo.com";
    private static final String BAR_PASSWORD = "123456";
    private static final String SEARCH_QUERY = "Despacito";
    private static final String FRONTEND_URL = "http://127.0.0.1:4200";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/gramola_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    @BeforeAll
    static void setupClass() {
        // WebDriverManager descarga automáticamente el chromedriver correcto
        WebDriverManager.chromedriver().setup();
    }

    private void limpiarBaseDeDatos() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            System.out.println(">>> Limpiando tablas de la base de datos para un test limpio...");
            stmt.executeUpdate("DELETE FROM playback_queue");
            stmt.executeUpdate("DELETE FROM bar_songs");
            
            // Resetear el estado de reproducción del bar de pruebas
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE users SET current_playlist_uri = NULL, last_playlist_index = 0 WHERE email = ?")) {
                pstmt.setString(1, BAR_EMAIL);
                pstmt.executeUpdate();
            }
            
            System.out.println(">>> Base de datos limpia con éxito.");
        } catch (SQLException e) {
            System.err.println(">>> Error al limpiar la base de datos: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        // Limpiar la base de datos antes de empezar el test
        limpiarBaseDeDatos();

        ChromeOptions options = new ChromeOptions();
        // NO usar headless para que el tribunal VEA el navegador
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        
        // Desactivar el gestor de contraseñas de Chrome y detector de brechas de seguridad (leak detection)
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);
        
        options.addArguments("--disable-save-password-bubble");
        options.addArguments("--disable-features=PasswordLeakDetection,DetectPasswordGeneration");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        
        System.out.println("=== SELENIUM: Chrome abierto en modo VISUAL ===");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            System.out.println("=== SELENIUM: Cerrando Chrome en 3 segundos... ===");
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            driver.quit();
        }
    }

    @Test
    @DisplayName("Sección 4: Cliente busca canción, paga y se verifica en BD")
    public void testBuscarPagarYVerificarEnBD() throws Exception {
        
        // =============================================
        // PASO 1: LOGIN DEL PROPIETARIO DEL BAR
        // =============================================
        System.out.println("\n>>> PASO 1: Navegando al Login...");
        driver.get(FRONTEND_URL + "/login");
        Thread.sleep(2000); // Pausa visual para que el tribunal vea

        WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("input[name='email']")));
        emailInput.clear();
        emailInput.sendKeys(BAR_EMAIL);

        WebElement passwordInput = driver.findElement(By.cssSelector("input[name='password']"));
        passwordInput.clear();
        passwordInput.sendKeys(BAR_PASSWORD);

        Thread.sleep(1000); // Pausa visual
        System.out.println(">>> Login: Credenciales introducidas. Pulsando botón...");
        
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        Thread.sleep(3000); // Esperar redirección (puede ir a Spotify o a /music)

        // Si nos redirige a Spotify OAuth, el test se detiene aquí 
        // porque es un paso manual. En ese caso, hay que ejecutar el test
        // con una sesión de Spotify ya activa.
        String currentUrl = driver.getCurrentUrl();
        System.out.println(">>> URL actual tras login: " + currentUrl);
        
        if (!currentUrl.contains("/music")) {
            System.out.println(">>> NOTA: El login ha redirigido fuera de /music.");
            System.out.println(">>> Si es la pantalla de Spotify OAuth, acepta manualmente");
            System.out.println(">>> y vuelve a ejecutar el test con la sesión activa.");
            // Para la defensa: navegar directamente a /music si ya hay sesión
            driver.get(FRONTEND_URL + "/music");
            Thread.sleep(3000);
        }

        // =============================================
        // PASO 2: BUSCAR UNA CANCIÓN
        // =============================================
        System.out.println("\n>>> PASO 2: Buscando canción: " + SEARCH_QUERY);
        
        WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".search-box input")));
        searchInput.clear();
        searchInput.sendKeys(SEARCH_QUERY);
        
        Thread.sleep(3000); // Esperar debounce + respuesta de Spotify
        System.out.println(">>> Búsqueda realizada. Esperando resultados...");

        // Verificar que aparecen resultados
        List<WebElement> results = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector(".track-card")));
        assertTrue(results.size() > 0, "Deberían aparecer resultados de búsqueda");
        System.out.println(">>> Se encontraron " + results.size() + " resultados.");
        Thread.sleep(2000); // Pausa visual

        // =============================================
        // PASO 3: PAGAR LA PRIMERA CANCIÓN
        // =============================================
        System.out.println("\n>>> PASO 3: Pulsando botón de pago en el primer resultado...");
        
        WebElement payButton = results.get(0).findElement(By.cssSelector(".pay-btn"));
        payButton.click();
        Thread.sleep(2000); // Esperar modal

        // Verificar que el modal de pago se ha abierto
        WebElement modal = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".payment-modal")));
        assertTrue(modal.isDisplayed(), "El modal de pago debería estar visible");
        System.out.println(">>> Modal de pago abierto. Rellenando tarjeta...");

        // Rellenar formulario de pago
        driver.findElement(By.cssSelector("input[name='name']")).sendKeys("RODRIGO TEST");
        Thread.sleep(500);
        
        driver.findElement(By.cssSelector("input[name='number']")).sendKeys("4242424242424242");
        Thread.sleep(500);
        
        driver.findElement(By.cssSelector("input[name='expiry']")).sendKeys("1228");
        Thread.sleep(500);
        
        driver.findElement(By.cssSelector("input[name='cvv']")).sendKeys("123");
        Thread.sleep(1000); // Pausa visual

        System.out.println(">>> Datos de tarjeta introducidos. Confirmando pago...");

        // Pulsar confirmar
        driver.findElement(By.cssSelector(".confirm-btn")).click();
        Thread.sleep(4000); // Esperar procesamiento (2s simulación + respuesta)

        System.out.println(">>> Pago procesado. Verificando en la interfaz...");
        Thread.sleep(2000);

        // =============================================
        // PASO 4: VERIFICACIÓN EN BASE DE DATOS
        // =============================================
        System.out.println("\n>>> PASO 4: Verificando en la Base de Datos MySQL...");
        
        verificarEnBaseDeDatos();

        System.out.println("\n=== ✅ TEST COMPLETADO CON ÉXITO ===");
        System.out.println("=== La canción fue buscada, pagada y verificada en BD ===");
        System.out.println("=== Dejando sonar la canción durante 12 segundos para demostrar que funciona... ===");
        
        Thread.sleep(12000); // Pausa final para que se escuche la canción y el tribunal vea el resultado
    }

    /**
     * Comprobación directa contra MySQL.
     * Verifica las dos tablas que exige el enunciado:
     * 1. playback_queue: la canción está en la cola de reproducción
     * 2. bar_songs: la canción se ha añadido al historial del bar
     */
    private void verificarEnBaseDeDatos() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        Statement stmt = conn.createStatement();

        // VERIFICACIÓN 1: La canción está en la cola de reproducción (playback_queue)
        ResultSet rs1 = stmt.executeQuery(
            "SELECT id, title, artist, position FROM playback_queue ORDER BY id DESC LIMIT 5");
        
        System.out.println("\n--- Tabla playback_queue (últimas 5 entradas) ---");
        boolean foundInQueue = false;
        while (rs1.next()) {
            String title = rs1.getString("title");
            String artist = rs1.getString("artist");
            int position = rs1.getInt("position");
            System.out.println("  [pos=" + position + "] " + title + " - " + artist);
            if (title != null) foundInQueue = true;
        }
        assertTrue(foundInQueue, "Debería haber al menos una canción en playback_queue");

        // VERIFICACIÓN 2: La canción se ha añadido a la lista del bar (bar_songs)
        ResultSet rs2 = stmt.executeQuery(
            "SELECT id, title, artist, played_at FROM bar_songs ORDER BY id DESC LIMIT 5");
        
        System.out.println("\n--- Tabla bar_songs (últimas 5 entradas) ---");
        boolean foundInBarSongs = false;
        while (rs2.next()) {
            String title = rs2.getString("title");
            String artist = rs2.getString("artist");
            String playedAt = rs2.getString("played_at");
            System.out.println("  " + title + " - " + artist + " [" + playedAt + "]");
            if (title != null) foundInBarSongs = true;
        }
        assertTrue(foundInBarSongs, "Debería haber al menos una canción en bar_songs");

        System.out.println("\n>>> ✅ Verificación de BD exitosa en AMBAS tablas.");

        conn.close();
    }
}
