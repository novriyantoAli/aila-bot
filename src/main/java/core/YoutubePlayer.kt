package core

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


class YoutubePlayer(val profileFolder: String, val profileLocation: String, private val playlistName: String, private val playlistCode: String, private val channelName: String) {
    companion object {
        const val URL = "https://www.youtube.com/"
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

        driver.manage().deleteAllCookies()

//        setStealth()

        wait = WebDriverWait(driver, Duration.ofSeconds(30))
    }

    private fun loadOptions(): ChromeOptions {
        val options = ChromeOptions()
        options.addArguments("start-maximized")
        options.addArguments("--user-data-dir=$profileLocation")
        options.addArguments("--profile-directory=$profileFolder")

        options.setExperimentalOption("useAutomationExtension", false)
        options.setExperimentalOption("excludeSwitches", listOf("enable-automation"))

        options.addArguments("--disable-blink-features=AutomationControlled")

        return options
    }

    private fun getDocumentMaxUpdate(js: JavascriptExecutor): Long {
        return js.executeScript("return (document.documentElement.scrollHeight - document.documentElement.clientHeight)") as Long
    }

    private fun setStealth() {
        val args = HashMap<String, Any?>()
        args[Stealth.Companion.KEYS.USER_AGENT.toString()] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
        args[Stealth.Companion.KEYS.LANGUAGES.toString()] = arrayOf("en-US", "en")
        args[Stealth.Companion.KEYS.VENDOR.toString()] = "Google Inc."
        args[Stealth.Companion.KEYS.PLATFORM.toString()] = "Win32"
        args[Stealth.Companion.KEYS.WEB_GL_VENDOR.toString()] = "Intel Inc."
        args[Stealth.Companion.KEYS.RENDERER.toString()] = "Intel Iris OpenGL Engine"
        args[Stealth.Companion.KEYS.FIX_HAIRLINE.toString()] = true

        Stealth(driver, args).makeStealth()
    }

    private fun waitDocumentReady() {
        do {
            val document = driver.executeScript("return document.readyState").toString()
            Thread.sleep(5000)
        } while (!document.equals("complete", ignoreCase = true))
    }


    fun goToHomePage() {
        driver.get(URL)

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("search_query")))
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


    fun searchAndGoFirst(searchWord: String) {

        goToHomePage()

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("search_query")))
        val searchBox = driver.findElement(By.name("search_query"))
        searchBox.clear()
        searchWord.forEach { searchBox.sendKeys(it.toString()) }
        searchBox.sendKeys(Keys.ENTER)

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[@href]")))
        val elements = driver.findElements(By.xpath("//a[@href]"))
        for (element in elements) {
            if (element?.getAttribute("href") != null && element.getAttribute("href").contains(channelName, ignoreCase = true) && element.getAttribute("id").equals("main-link", ignoreCase = true)) {
                driver.executeScript("arguments[0].click();", element)
                break
            }
        }

        waitDocumentReady()
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tp-yt-paper-tab")));
        val tabs = driver.findElements(By.className("tp-yt-paper-tab"))
        for (tab in tabs) {
            if (tab.text.trim().contains("PLAYLIST", ignoreCase = true)) {
                tab.click()
                break
            }
        }

        waitDocumentReady()//span[contains(text(), '144p')]
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//a[@href]")))
        val playlists = driver.findElements(By.xpath("//a[@href]"))
        for (playlist in playlists) {
            if (playlist != null && playlist.getAttribute("href").contains(playlistCode, ignoreCase = true) && playlist.getAttribute("title").contains(playlistName, ignoreCase = true)) {
                driver.executeScript("arguments[0].click();", playlist)
                break
            }
        }
    }

    fun changeResolution() {
        try {
            val settings = driver.findElement(
                By.xpath("//button[@data-tooltip-target-id='ytp-settings-button']")
            )
            settings.click()

            waitDocumentReady()
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("ytp-menu-label-secondary")))
            val items = driver.findElements(By.className("ytp-menu-label-secondary"))
            for (item in items) {
                if (!item.text.contains("144p", ignoreCase = true)) {
                    item.click()
                    break
                }
            }

            waitDocumentReady()
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//span[contains(text(),'144p')]")))
            val span144p = driver.findElements(By.xpath("//span[contains(text(), '144p')]"))
            hasMinimalQuality = if (span144p.size < 2) {
                span144p[0].click()
                true
            } else { true }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkPlaylistEnd(): Boolean {
        return try {
            waitDocumentReady()
            if (driver.currentUrl.contains(playlistCode, ignoreCase = true))
                return false
            true
        } catch (e: Exception){ e.printStackTrace(); true }
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