package com.revature;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.javalin.Javalin;

public class AdminTest {
    
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static Javalin app;
    private static JavascriptExecutor js;
    private static final Logger logger = Logger.getLogger(AdminTest.class.getName());
    private static Process httpServerProcess;
    private static String browserType;
    
    // Architecture and system detection
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();
    private static final boolean IS_ARM = OS_ARCH.contains("aarch64") || OS_ARCH.contains("arm");
    private static final boolean IS_WINDOWS = OS_NAME.contains("windows");
    private static final boolean IS_LINUX = OS_NAME.contains("linux");
    private static final boolean IS_MAC = OS_NAME.contains("mac");

    @BeforeClass
    public static void setUp() throws InterruptedException {
        try {
            printEnvironmentInfo();
            
            // Start the backend programmatically
            int port = 8081;
            app = Main.main(new String[] { String.valueOf(port) });
            
            // Detect browser and driver
            BrowserConfig browserConfig = detectBrowserAndDriver();
            browserType = browserConfig.browserType;
            
            // Create WebDriver with appropriate configuration
            driver = createWebDriver(browserConfig);
            
            // Initialize WebDriverWait
            wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            
            // Set timeouts
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            js = (JavascriptExecutor) driver;
            
            Thread.sleep(1000);
            
        } catch (Exception e) {
            System.err.println("\n=== SETUP FAILED ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            
            cleanup();
            throw new RuntimeException("Setup failed", e);
        }
    }

    private static void printEnvironmentInfo() {
        System.out.println("=== ENVIRONMENT INFO ===");
        System.out.println("OS: " + OS_NAME + " (" + OS_ARCH + ")");
        System.out.println("Architecture: " + (IS_ARM ? "ARM64" : "x86/x64"));
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Working directory: " + System.getProperty("user.dir"));
    }

    private static BrowserConfig detectBrowserAndDriver() {
        System.out.println("\n=== BROWSER AND DRIVER DETECTION ===");
        
        // First check for driver in project's "driver" folder
        BrowserConfig projectDriverConfig = checkProjectDriverFolder();
        if (projectDriverConfig != null) {
            return projectDriverConfig;
        }
        
        // Then check system-installed drivers
        BrowserConfig systemDriverConfig = checkSystemDrivers();
        if (systemDriverConfig != null) {
            return systemDriverConfig;
        }
        
        throw new RuntimeException("No compatible browser driver found");
    }
    
    private static BrowserConfig checkProjectDriverFolder() {
        File driverFolder = new File("driver");
        if (!driverFolder.exists() || !driverFolder.isDirectory()) {
            System.out.println("No 'driver' folder found in project root");
            return null;
        }
        
        System.out.println("Found 'driver' folder, checking for executables...");
        
        // Check for Edge driver first
        String[] edgeDriverNames = IS_WINDOWS ? 
            new String[]{"msedgedriver.exe", "edgedriver.exe"} :
            new String[]{"msedgedriver", "edgedriver"};
            
        for (String driverName : edgeDriverNames) {
            File driverFile = new File(driverFolder, driverName);
            if (driverFile.exists()) {
                makeExecutable(driverFile);
                if (driverFile.canExecute()) {
                    System.out.println("Found Edge driver: " + driverFile.getAbsolutePath());
                    return new BrowserConfig("edge", driverFile.getAbsolutePath(), findEdgeBinary());
                }
            }
        }
        
        // Check for Chrome driver
        String[] chromeDriverNames = IS_WINDOWS ? 
            new String[]{"chromedriver.exe"} :
            new String[]{"chromedriver"};
            
        for (String driverName : chromeDriverNames) {
            File driverFile = new File(driverFolder, driverName);
            if (driverFile.exists()) {
                makeExecutable(driverFile);
                if (driverFile.canExecute()) {
                    System.out.println("Found Chrome driver: " + driverFile.getAbsolutePath());
                    return new BrowserConfig("chrome", driverFile.getAbsolutePath(), findChromeBinary());
                }
            }
        }
        
        System.out.println("No compatible drivers found in 'driver' folder");
        return null;
    }
    
    private static BrowserConfig checkSystemDrivers() {
        System.out.println("Checking system-installed drivers...");
        
        // Chrome driver paths
        String[] chromeDriverPaths = {
            "/usr/bin/chromedriver",
            "/usr/local/bin/chromedriver",
            "/snap/bin/chromedriver",
            System.getProperty("user.home") + "/.cache/selenium/chromedriver/linux64/chromedriver",
            "/opt/chromedriver/chromedriver"
        };
        
        if (IS_WINDOWS) {
            chromeDriverPaths = new String[]{
                "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver.exe",
                "C:\\ChromeDriver\\chromedriver.exe",
                "chromedriver.exe"
            };
        }
        
        for (String driverPath : chromeDriverPaths) {
            File driverFile = new File(driverPath);
            if (driverFile.exists() && driverFile.canExecute()) {
                System.out.println("Found system Chrome driver: " + driverPath);
                return new BrowserConfig("chrome", driverPath, findChromeBinary());
            }
        }
        
        // Edge driver paths
        if (IS_WINDOWS) {
            String[] edgeDriverPaths = {
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedgedriver.exe",
                "msedgedriver.exe"
            };
            
            for (String driverPath : edgeDriverPaths) {
                File driverFile = new File(driverPath);
                if (driverFile.exists() && driverFile.canExecute()) {
                    System.out.println("Found system Edge driver: " + driverPath);
                    return new BrowserConfig("edge", driverPath, findEdgeBinary());
                }
            }
        }
        
        return null;
    }
    
    private static String findChromeBinary() {
        String[] chromePaths;
        
        if (IS_WINDOWS) {
            chromePaths = new String[]{
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
            };
        } else if (IS_MAC) {
            chromePaths = new String[]{
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
            };
        } else {
            chromePaths = new String[]{
                "/usr/bin/chromium-browser",
                "/usr/bin/chromium",
                "/usr/bin/google-chrome",
                "/snap/bin/chromium"
            };
        }
        
        for (String path : chromePaths) {
            if (new File(path).exists()) {
                System.out.println("Found Chrome binary: " + path);
                return path;
            }
        }
        
        System.out.println("Chrome binary not found, using default");
        return null;
    }
    
    private static String findEdgeBinary() {
        if (IS_WINDOWS) {
            String[] edgePaths = {
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe"
            };
            
            for (String path : edgePaths) {
                if (new File(path).exists()) {
                    System.out.println("Found Edge binary: " + path);
                    return path;
                }
            }
        }
        
        System.out.println("Edge binary not found, using default");
        return null;
    }
    
    private static void makeExecutable(File file) {
        if (!file.canExecute()) {
            try {
                file.setExecutable(true);
                System.out.println("Made executable: " + file.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("Could not make executable: " + e.getMessage());
            }
        }
    }
    
    private static WebDriver createWebDriver(BrowserConfig config) {
        System.out.println("\n=== CREATING WEBDRIVER ===");
        System.out.println("Browser: " + config.browserType);
        System.out.println("Driver: " + config.driverPath);
        System.out.println("Binary: " + config.binaryPath);
        
        if ("edge".equals(config.browserType)) {
            return createEdgeDriver(config);
        } else {
            return createChromeDriver(config);
        }
    }
    
    private static WebDriver createChromeDriver(BrowserConfig config) {
        System.setProperty("webdriver.chrome.driver", config.driverPath);
        
        ChromeOptions options = new ChromeOptions();
        
        if (config.binaryPath != null) {
            options.setBinary(config.binaryPath);
        }
        
        options.addArguments(getChromeArguments());
        
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logPrefs);
        
        ChromeDriverService.Builder serviceBuilder = new ChromeDriverService.Builder()
            .usingDriverExecutable(new File(config.driverPath))
            .withTimeout(Duration.ofSeconds(30));
        
        ChromeDriverService service = serviceBuilder.build();
        
        return new ChromeDriver(service, options);
    }
    
    private static WebDriver createEdgeDriver(BrowserConfig config) {
        System.setProperty("webdriver.edge.driver", config.driverPath);
        
        EdgeOptions options = new EdgeOptions();
        
        if (config.binaryPath != null) {
            options.setBinary(config.binaryPath);
        }
        
        options.addArguments(getEdgeArguments());
        
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("ms:loggingPrefs", logPrefs);
        
        EdgeDriverService.Builder serviceBuilder = new EdgeDriverService.Builder()
            .usingDriverExecutable(new File(config.driverPath))
            .withTimeout(Duration.ofSeconds(30));
        
        EdgeDriverService service = serviceBuilder.build();
        
        return new EdgeDriver(service, options);
    }
    
    private static String[] getChromeArguments() {
        return getCommonBrowserArguments();
    }
    
    private static String[] getEdgeArguments() {
        return getCommonBrowserArguments();
    }
    
    private static String[] getCommonBrowserArguments() {
        String[] baseArgs = {
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--disable-web-security",
            "--allow-file-access-from-files",
            "--allow-running-insecure-content",
            "--user-data-dir=/tmp/browser-test-" + System.currentTimeMillis(),
            "--disable-features=TranslateUI,VizDisplayCompositor",
            "--disable-background-timer-throttling",
            "--disable-backgrounding-occluded-windows",
            "--disable-renderer-backgrounding"
        };
        
        if (IS_ARM) {
            String[] armArgs = {
                "--disable-features=VizDisplayCompositor",
                "--use-gl=swiftshader",
                "--disable-software-rasterizer"
            };
            
            String[] combined = new String[baseArgs.length + armArgs.length];
            System.arraycopy(baseArgs, 0, combined, 0, baseArgs.length);
            System.arraycopy(armArgs, 0, combined, baseArgs.length, armArgs.length);
            return combined;
        }
        
        return baseArgs;
    }
    
    private static void cleanup() {
        if (app != null) {
            app.stop();
        }
        if (driver != null) {
            try {
                driver.quit();
                driver = null;
            } catch (Exception e) {
                System.err.println("Error cleaning up WebDriver: " + e.getMessage());
            }
        }
    }

    @AfterClass
    public static void tearDown() {
        System.out.println("\n=== TEARDOWN ===");
        cleanup();
        System.out.println("Teardown completed");
    }
    
    private static class BrowserConfig {
        final String browserType;
        final String driverPath;
        final String binaryPath;
        
        BrowserConfig(String browserType, String driverPath, String binaryPath) {
            this.browserType = browserType;
            this.driverPath = driverPath;
            this.binaryPath = binaryPath;
        }
    }

    private static void performLogout() {
        WebElement logoutButton = driver.findElement(By.id("logout-button"));
        logoutButton.click();
    }

    /**
     * Admin link should not exist when the logged-in user is not an admin.
     * @throws InterruptedException
     */
    @Test
    public void noAdminNoLinkTest() throws InterruptedException{
        // go to relevant HTML page
        File loginFile = new File("src/main/resources/public/frontend/login/login-page.html");
        String loginPath = "file:///" + loginFile.getAbsolutePath().replace("\\", "/");
        driver.get(loginPath);


        try {
    WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
    Alert alert = shortWait.until(ExpectedConditions.alertIsPresent());
    System.out.println("Dismissing leftover alert: " + alert.getText());
    alert.dismiss();
} catch (Exception ignored) {
    // No alert present — move on
}

        // perform login functionality
        WebElement usernameInput = driver.findElement(By.id("login-input"));
        WebElement passwordInput = driver.findElement(By.id("password-input"));
        WebElement loginButton = driver.findElement(By.id("login-button"));
        usernameInput.sendKeys("JoeCool");
        passwordInput.sendKeys("redbarron");
        loginButton.click();

        // ensure we navigate to appropriate webpage
        wait.until(ExpectedConditions.urlContains("recipe-page")); // Wait for navigation to the recipe page
    
        // check that role value is 'false' in session storage
        assertTrue((js.executeScript(String.format(
            "return window.sessionStorage.getItem('%s');", "is-admin")).equals("false")));
        
        WebElement adminLink = driver.findElement(By.id("admin-link"));
        //verify that there are no admin links because the user is not an admin.
        Assert.assertFalse(adminLink.isDisplayed());

        performLogout();
    }

    /**
     * Admin link should exist when the logged-in user is an admin.
     * @throws InterruptedException
     */
    @Test
    public void adminLinkTest() throws InterruptedException{
        // go to relevant HTML page
        File loginFile = new File("src/main/resources/public/frontend/login/login-page.html");
        String loginPath = "file:///" + loginFile.getAbsolutePath().replace("\\", "/");
        driver.get(loginPath);

        // perform login functionality
        WebElement usernameInput = driver.findElement(By.id("login-input"));
        WebElement passwordInput = driver.findElement(By.id("password-input"));
        WebElement loginButton = driver.findElement(By.id("login-button"));
        usernameInput.sendKeys("ChefTrevin");
        passwordInput.sendKeys("trevature");
        loginButton.click();

        // ensure we navigate to appropriate webpage
        wait.until(ExpectedConditions.urlContains("recipe-page")); // Wait for navigation to the recipe page
    
        // check that role value is 'false' in session storage
        assertTrue((js.executeScript(String.format(
            "return window.sessionStorage.getItem('%s');", "is-admin")).equals("true")));
        
        WebElement adminLink = driver.findElement(By.id("admin-link"));
        //verify that there are no admin links because the user is not an admin.
        Assert.assertTrue(adminLink.isDisplayed());

        performLogout();
    }

    /**
     * On startup, the site should pull the currently available ingredients from the API.
     */
    @Test
    public void displayIngredientsOnInitTest() throws InterruptedException{
        // go to relevant HTML page
        File loginFile = new File("src/main/resources/public/frontend/login/login-page.html");
        String loginPath = "file:///" + loginFile.getAbsolutePath().replace("\\", "/");
        driver.get(loginPath);

        // perform login functionality
        WebElement usernameInput = driver.findElement(By.id("login-input"));
        WebElement passwordInput = driver.findElement(By.id("password-input"));
        WebElement loginButton = driver.findElement(By.id("login-button"));
        usernameInput.sendKeys("ChefTrevin");
        passwordInput.sendKeys("trevature");
        loginButton.click();

        // ensure we navigate to appropriate webpage
        wait.until(ExpectedConditions.urlContains("recipe-page")); // Wait for navigation to the recipe page
    
        // click on link to go to ingredients page
        Thread.sleep(1000);
        WebElement adminLink = driver.findElement(By.id("admin-link"));
        adminLink.click();

        Thread.sleep(1000);
        WebElement list = driver.findElement(By.id("ingredient-list"));
        String innerString = list.getAttribute("innerHTML");
        Assert.assertTrue(innerString.contains("carrot"));
        Assert.assertTrue(innerString.contains("potato"));
        Assert.assertTrue(innerString.contains("tomato"));
        Assert.assertTrue(innerString.contains("lemon"));
        Assert.assertTrue(innerString.contains("rice"));
        Assert.assertTrue(innerString.contains("stone"));

        WebElement backLink = driver.findElement(By.id("back-link"));
        backLink.click();
        Thread.sleep(1000);

        performLogout();
    }

    /**
     * The site should send a request to persist the ingredient after the recipe is submitted.
     * @throws InterruptedException
     */
    @Test
    public void addIngredientPostTest() throws InterruptedException{
        // go to relevant HTML page
        File loginFile = new File("src/main/resources/public/frontend/login/login-page.html");
        String loginPath = "file:///" + loginFile.getAbsolutePath().replace("\\", "/");
        driver.get(loginPath);

        // perform login functionality
        WebElement usernameInput = driver.findElement(By.id("login-input"));
        WebElement passwordInput = driver.findElement(By.id("password-input"));
        WebElement loginButton = driver.findElement(By.id("login-button"));
        usernameInput.sendKeys("ChefTrevin");
        passwordInput.sendKeys("trevature");
        loginButton.click();

        // ensure we navigate to appropriate webpage
        wait.until(ExpectedConditions.urlContains("recipe-page")); // Wait for navigation to the recipe page
    
        // click on link to go to ingredients page
        Thread.sleep(1000);
        WebElement adminLink = driver.findElement(By.id("admin-link"));
        adminLink.click();

        Thread.sleep(1000);
        
        WebElement nameInput = driver.findElement(By.id("add-ingredient-name-input"));
        WebElement ingredientSubmitButton = driver.findElement(By.id("add-ingredient-submit-button"));
        nameInput.sendKeys("salt");
        ingredientSubmitButton.click();
        Thread.sleep(1000);

        // Wait for the recipe list to update
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ingredient-list")));
        WebElement ingredientList = driver.findElement(By.id("ingredient-list"));
        String innerHTML = ingredientList.getAttribute("innerHTML");

        // Assert the result
        assertTrue("Expected ingredient to be added.", innerHTML.contains("salt"));

        WebElement backLink = driver.findElement(By.id("back-link"));
        backLink.click();
        Thread.sleep(1000);

        performLogout();
    }

    /**
     * The site should send a request to delete the ingredient when the delete button is clicked.
     * @throws InterruptedException
     */
    @Test
    public void deleteIngredientDeleteTest() throws InterruptedException{
        // go to relevant HTML page
        File loginFile = new File("src/main/resources/public/frontend/login/login-page.html");
        String loginPath = "file:///" + loginFile.getAbsolutePath().replace("\\", "/");
        driver.get(loginPath);

        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            Alert alert = shortWait.until(ExpectedConditions.alertIsPresent());
            System.out.println("Dismissing leftover alert: " + alert.getText());
            alert.dismiss();
        } catch (Exception ignored) {
            // No alert present — move on
        }

        // perform login functionality
        WebElement usernameInput = driver.findElement(By.id("login-input"));
        WebElement passwordInput = driver.findElement(By.id("password-input"));
        WebElement loginButton = driver.findElement(By.id("login-button"));
        usernameInput.sendKeys("ChefTrevin");
        passwordInput.sendKeys("trevature");
        loginButton.click();

        // ensure we navigate to appropriate webpage
        wait.until(ExpectedConditions.urlContains("recipe-page")); // Wait for navigation to the recipe page
    
        // click on link to go to ingredients page
        Thread.sleep(1000);
        WebElement adminLink = driver.findElement(By.id("admin-link"));
        adminLink.click();

        Thread.sleep(1000);
        
        WebElement nameInput = driver.findElement(By.id("delete-ingredient-name-input"));
        WebElement ingredientSubmitButton = driver.findElement(By.id("delete-ingredient-submit-button"));
        nameInput.sendKeys("tomato");
        ingredientSubmitButton.click();
        Thread.sleep(1000);
        
        // Wait for the recipe list to update
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ingredient-list")));
        WebElement ingredientList = driver.findElement(By.id("ingredient-list"));
        String innerHTML = ingredientList.getAttribute("innerHTML");

        // Assert the result
        assertTrue("Expected ingredient to NOT be added.", !innerHTML.contains("tomato"));

        WebElement backLink = driver.findElement(By.id("back-link"));
        backLink.click();
        Thread.sleep(1000);

        performLogout();
    }

}
