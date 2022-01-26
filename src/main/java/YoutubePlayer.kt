import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import stealth.Stealth
import java.time.Duration
import kotlin.NoSuchElementException
import kotlin.system.exitProcess


class YoutubePlayer(val username: String?, val password: String?, private val playlistHome: String, private val playlistWatch: String, private val profile: String, private val channel: String) {
    companion object {
        const val URL = "https://www.youtube.com"
        const val URL_LOGIN = "https://stackoverflow.com"
        const val PIXEL = 100
    }
    private var driver: ChromeDriver
    private var wait: WebDriverWait

    var current: String?
    var duration: String?
    var titleNow: String?
    var hasAd: Boolean
    var endPage: Boolean
    var hasMinimalQuality: Boolean

    init {
        hasAd = false
        endPage = false
        current = null
        duration = null
        titleNow = null
        hasMinimalQuality = false

        driver = ChromeDriver(loadOptions())
        wait = WebDriverWait(driver, Duration.ofSeconds(30))
    }

    fun goToLoginProcedure() {
        val args = HashMap<String, Any?>()
        args[Stealth.Companion.KEYS.USER_AGENT.toString()] = "DN"
        args[Stealth.Companion.KEYS.LANGUAGES.toString()] = listOf("en-US", "en")
        args[Stealth.Companion.KEYS.VENDOR.toString()] = "Google Inc."
        args[Stealth.Companion.KEYS.PLATFORM.toString()] = "Win32"
        args[Stealth.Companion.KEYS.WEB_GL_VENDOR.toString()] = "Intel Inc."
        args[Stealth.Companion.KEYS.RENDERER.toString()] = "Intel Iris OpenGL Engine"
        args[Stealth.Companion.KEYS.FIX_HAIRLINE.toString()] = true

        val stealth = Stealth(driver, args)
        stealth.makeStealth()

        driver.get(URL_LOGIN)

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[@href='https://stackoverflow.com/users/login?ssrc=head&returnurl=https%3a%2f%2fstackoverflow.com%2f']")))
        val btnLogin = driver.findElement(By.xpath("//a[@href='https://stackoverflow.com/users/login?ssrc=head&returnurl=https%3a%2f%2fstackoverflow.com%2f']"))
        btnLogin.click()

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@data-oauthserver='https://accounts.google.com/o/oauth2/auth']")))
        val btnGoogle = driver.findElement(By.xpath("//button[@data-oauthserver='https://accounts.google.com/o/oauth2/auth']"))
        btnGoogle.click()

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Email")))
        val emailField = driver.findElement(By.id("Email"))
        username?.forEach { emailField.sendKeys(it.toString()) }
        emailField.sendKeys(Keys.ENTER)

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password")))
        val passwordField = driver.findElement(By.id("password"))
        password?.forEach { passwordField.sendKeys(it.toString()) }
        passwordField.sendKeys(Keys.ENTER)

        waitDocumentReady()
        val pagePermission = driver.findElements(By.id("partiesBrand"))
        if (pagePermission.isEmpty()) {
            driver.quit()
            exitProcess(0)
        }
    }

    fun goToHomePage(login: Boolean) {
        if (login) {
            driver.executeScript("window.open('${URL}')")
            val list = ArrayList(driver.windowHandles)
            driver.switchTo().window(list[0]).close()
            driver.switchTo().window(list[1])
        } else
            driver.get(URL)

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("search_query")))
    }

    private fun loadOptions(): ChromeOptions {
        val options = ChromeOptions()
        options.addArguments("start-maximized")
        options.setExperimentalOption("useAutomationExtension", false)
        options.setExperimentalOption("excludeSwitches", listOf("enable-automation"))
        options.addArguments("user-agent=DN")
        options.addArguments("user-data-dir=$profile")

        return options
    }

    private fun getDocumentMaxUpdate(js: JavascriptExecutor): Long {
        return js.executeScript("return (document.documentElement.scrollHeight - document.documentElement.clientHeight)") as Long
    }

    fun lookupRecommendation() {
        val js = driver as JavascriptExecutor
        var scrollHeight = getDocumentMaxUpdate(js)
        var scrolledPage = 0
        while ((js.executeScript("return window.pageYOffset") as Long) < scrollHeight ) {
            js.executeScript("window.scrollBy(0, $PIXEL)")
            scrolledPage += PIXEL
            scrollHeight = getDocumentMaxUpdate(js)
            if ((scrolledPage + PIXEL) > scrollHeight )
                Thread.sleep(5000)
        }
    }

    private fun waitDocumentReady() {
        do {
            val document = driver.executeScript("return document.readyState").toString()
            Thread.sleep(5000)
        } while (!document.equals("complete", ignoreCase = true))
    }

    fun searchAndGoFirst(searchWord: String) {
        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("search_query")))
        val searchBox = driver.findElement(By.name("search_query"))
        searchBox.clear()
        searchWord.forEach { searchBox.sendKeys(it.toString()) }
        searchBox.sendKeys(Keys.ENTER)

        waitDocumentReady()

        val elements = driver.findElements(By.xpath("//a[@href]"))
        for (element in elements) {
            if (element?.getAttribute("href") != null && element.getAttribute("href").contains(channel, ignoreCase = true)) {
                driver.executeScript("arguments[0].click();", element)
                break
            }
        }

        waitDocumentReady()

        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("tp-yt-paper-tab")));
        val tabs = driver.findElements(By.className("tp-yt-paper-tab"))
        for (tab in tabs) {
            if (tab.text.trim().contains("PLAYLIST", ignoreCase = true)) {
                tab.click()
                break
            }
        }

        waitDocumentReady()

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@href='$playlistWatch']")))
        val playlist = driver.findElement(By.xpath("//a[@href='${playlistWatch}']"))
        driver.executeScript("arguments[0].click();", playlist)
    }

    fun changeResolution() {
        try {
            val settings = driver.findElement(
                By.xpath("//button[@data-tooltip-target-id='ytp-settings-button']")
            )
            settings.click()

            waitDocumentReady()
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("ytp-menu-label-secondary")))
            val items = driver.findElements(By.className("ytp-menu-label-secondary"))
            for (item in items) {
                if (!item.text.contains("144p", ignoreCase = true)) {
                    item.click()
                    break
                }
            }

            waitDocumentReady()

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(text(),'144p')]")))
            val span144p = driver.findElements(By.xpath("//span[contains(text(), '144p')]"))
            hasMinimalQuality = if (span144p.size < 2) {
                span144p[0].click()
                true
            } else { true }

        } catch (e: Exception) { e.printStackTrace() }

    }

    fun checkPlaylistEnd(playlistName: String): String? {
        return try {

            waitDocumentReady()

            val playlists = driver.findElements(By.xpath("//a[@href='${this.playlistHome}']"))
            for (playlist in playlists) {
                if (playlist.text.equals(playlistName, ignoreCase = true)) { return playlist.text }
            }
            return null
        } catch (e: Exception){ e.printStackTrace(); null }
    }

    private fun clickClearCookieEvent(ariaLabelLowerCase: String, ariaLabelUpperCase: String) {
        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tp-yt-paper-button")))
        val buttons = driver.findElements(By.cssSelector("tp-yt-paper-button"))
        for (button in buttons) {
            val ariaLabel: String? = button.getAttribute("aria-label")
            if (ariaLabel != null && ariaLabel.contains(ariaLabelLowerCase, ignoreCase = true)) {
                button.click()

                waitDocumentReady()
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tp-yt-paper-button[@aria-label='$ariaLabelUpperCase']")))
                val buttonModalClear = driver.findElement(By.xpath("//tp-yt-paper-button[@aria-label='$ariaLabelUpperCase']"))
                buttonModalClear.click()

                break
            }
        }
    }

    fun clearHistory() {
        driver.get("https://www.youtube.com/feed/history")

        clickClearCookieEvent("clear all watch history", "CLEAR WATCH HISTORY")

        clickClearCookieEvent("clear all search history", "CLEAR SEARCH HISTORY")
    }

    fun clearCookie() {
//        val args = HashMap<String, Any?>()
//        args[Stealth.Companion.KEYS.USER_AGENT.toString()] = "DN"
//        args[Stealth.Companion.KEYS.LANGUAGES.toString()] = listOf("en-US", "en")
//        args[Stealth.Companion.KEYS.VENDOR.toString()] = "Google Inc."
//        args[Stealth.Companion.KEYS.PLATFORM.toString()] = "Win32"
//        args[Stealth.Companion.KEYS.WEB_GL_VENDOR.toString()] = "Intel Inc."
//        args[Stealth.Companion.KEYS.RENDERER.toString()] = "Intel Iris OpenGL Engine"
//        args[Stealth.Companion.KEYS.FIX_HAIRLINE.toString()] = true
//
//        val stealth = Stealth(driver, args)
//        stealth.makeStealth()
//        driver.get("https://bot.sannysoft.com/")
//        Thread.sleep(5000)
//        System.exit(0)

//        waitDocumentReady()
//        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tp-yt-paper-button")))
//        val buttons = driver.findElements(By.cssSelector("tp-yt-paper-button"))
//        for (button in buttons) {
//            val ariaLabel: String? = button.getAttribute("aria-label")
//            if (ariaLabel != null && ariaLabel.contains("clear all watch history", ignoreCase = true)) {
//                button.click()
//
//                waitDocumentReady()
//
//                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tp-yt-paper-button[@aria-label='CLEAR WATCH HISTORY']")))
//                val buttonModalClear = driver.findElement(By.xpath("//tp-yt-paper-button[@aria-label='CLEAR WATCH HISTORY']"))
//                buttonModalClear.click()
//
//                break
//            }
//        }
//
//        waitDocumentReady()
//        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tp-yt-paper-button")))
//        val buttons2 = driver.findElements(By.cssSelector("tp-yt-paper-button"))
//
//        for (button in buttons2) {
//            val ariaLabel: String? = button.getAttribute("aria-label")
//            if (ariaLabel != null && ariaLabel.contains("clear all search history", ignoreCase = true)) {
//                button.click()
//
//                waitDocumentReady()
//
//                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//tp-yt-paper-button[@aria-label='CLEAR SEARCH HISTORY']")))
//                val buttonModalClear = driver.findElement(By.xpath("//tp-yt-paper-button[@aria-label='CLEAR SEARCH HISTORY']"))
//                buttonModalClear.click()
//
//                break
//            }
//        }
        driver.manage().deleteAllCookies()

        driver.get("chrome://settings/clearBrowserData")

        waitDocumentReady()

        val shadowRoot1 = driver.findElement(By.cssSelector("settings-ui")).shadowRoot
        val shadowRoot2 = shadowRoot1.findElement(By.cssSelector("settings-main")).shadowRoot
        val shadowRoot3 = shadowRoot2.findElement(By.cssSelector("settings-basic-page")).shadowRoot
        val shadowRoot4 = shadowRoot3.findElement(By.cssSelector("settings-section > settings-privacy-page")).shadowRoot
        val shadowRoot5 = shadowRoot4.findElement(By.cssSelector("settings-clear-browsing-data-dialog")).shadowRoot
        val root6 = shadowRoot5.findElement(By.cssSelector("#clearBrowsingDataDialog"))
        selectTimeRangeAll(root6)
        val clearDataButton = root6.findElement(By.cssSelector("#clearBrowsingDataConfirm"))

        clearDataButton.click()

        hasMinimalQuality = false

        Thread.sleep(30000)
    }

    private fun selectTimeRangeAll(root: WebElement?) {
        val locator1 = "iron-pages#tabs"
        val locator2 = "settings-dropdown-menu#clearFromBasic"
        val locator3 = "select#dropdownMenu"
        val locator4 = "4"

        val shadowRoot2 = findElementByCss(findElementByCss(root, locator1), locator2)?.shadowRoot
        val ddnTimeRange = shadowRoot2?.findElement(By.cssSelector(locator3))
        Select(ddnTimeRange).selectByValue(locator4)
    }

    private fun findElementByCss(element: SearchContext?, locator: String): WebElement? {
        var elapsed = 0
        val timeout = 1000
        val pollingInterval = 200
        do {
          try {
              val el = element?.findElement(By.cssSelector(locator))
              if (el != null)
                  return el
          } catch (e: NoSuchElementException){
              Thread.sleep(400)
              continue
          } finally { elapsed += pollingInterval }
        } while (elapsed < timeout)

        return null
    }

    fun checkAd(): String? {
        return try {
            val ad = driver.findElement(By.className("ytp-ad-preview-text"))
            hasAd = true
            ad.text
        } catch (e: Exception) { e.printStackTrace() ; null }
    }

    fun skipAd() {
        try {
            driver.findElement(By.className("ytp-ad-skip-button")).click()
            hasAd = false
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun exit() {
        driver.quit()
    }

}