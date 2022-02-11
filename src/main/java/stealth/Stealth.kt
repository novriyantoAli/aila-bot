package stealth

import org.openqa.selenium.chrome.ChromeDriver
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Stealth(private val driver: ChromeDriver, private val args: Map<String, Any?>) {

    private val uaLanguage: String = if (args.containsKey(KEYS.LANGUAGES.toString()) && args.getValue(KEYS.LANGUAGES.toString()) != null)
        (args.getValue(KEYS.LANGUAGES.toString()) as Array<*>).joinToString(separator = ",") { "'${it}'"}
    else
        arrayOf("en-US", "en").joinToString(separator = ",") { "'${it}'" }

    fun makeStealth() {
        // WITH_UTILS
        evaluateOnNewDocument(WITH_UTILS)
        // CHROME_APP
        evaluateOnNewDocument(CHROME_APP)
        // CHROME_RUNTIME
        chromeRuntime()
        // content_window
        evaluateOnNewDocument(IFRAME_CONTENT_WINDOW)
        // media.codecs.js
        evaluateOnNewDocument(MEDIA_CODECS)
        //
        navigatorLanguage()
        // navigator.permissions.js
        evaluateOnNewDocument(NAVIGATOR_PERMISSIONS)
        // navigator.plugins.js
        evaluateOnNewDocument(NAVIGATOR_PLUGINS)
        // navigator.vendor.js
        navigatorVendor()
        // navigator.webdriver.js
        evaluateOnNewDocument(NAVIGATOR_WEB_DRIVER)
        // user_agent_override
        userAgentOverride()
        // web_gl_vendor_override
        webGlVendorOverride()
        // window.outerdimensions.js
        evaluateOnNewDocument(WINDOW_OUTER_DIMENSION)
        if (args.containsKey(KEYS.FIX_HAIRLINE.toString()) && args.getValue(KEYS.FIX_HAIRLINE.toString()) as Boolean) {
            evaluateOnNewDocument(HAIR_LINE_FIX)
        }
    }

    private fun chromeRuntime() {
        val runOnInSecure = if (args.containsKey(KEYS.RUN_ON_IN_SECURE_ORIGINS.toString()) && args.getValue(KEYS.RUN_ON_IN_SECURE_ORIGINS.toString()) != null)
            args.getValue(KEYS.RUN_ON_IN_SECURE_ORIGINS.toString()) as Boolean
        else
            false

        evaluateOnNewDocument(CHROME_RUNTIME, runOnInSecure)
    }

    private fun navigatorLanguage() {
        val languages = if (args.containsKey(KEYS.LANGUAGES.toString()) && args.getValue(KEYS.LANGUAGES.toString()) != null)
            args.getValue(KEYS.LANGUAGES.toString()) as Array<*>
        else
            arrayOf("en-US", "en")

        evaluateOnNewDocument(NAVIGATOR_LANGUAGE, languages)
    }

    private fun navigatorVendor() {
        val vendor = if (args.containsKey(KEYS.VENDOR.toString()) && args.getValue(KEYS.VENDOR.toString()) != null)
            args.getValue(KEYS.VENDOR.toString()) as String
        else "Google Inc."

        evaluateOnNewDocument(NAVIGATOR_VENDOR, vendor)
    }

    private fun userAgentOverride() {
        var argUa = if (args.containsKey(KEYS.USER_AGENT.toString()) && args.getValue(KEYS.USER_AGENT.toString()) != null)
            args.getValue(KEYS.USER_AGENT.toString()) as String
        else driver.executeCdpCommand("Browser.getVersion", HashMap<String, Any>())["userAgent"] as String

        val platform: String? = if (args.containsKey(KEYS.PLATFORM.toString()) && args.getValue(KEYS.PLATFORM.toString()) != null)
            args.getValue(KEYS.PLATFORM.toString()) as String
        else null

        argUa = argUa.replace("HeadlessChrome", "Chrome")
        val map = HashMap<String, Any>()
        map["userAgent"] = argUa
        map["acceptLanguage"] = uaLanguage

        if (platform != null) {
            map["platform"] = platform.toString()
        }

        driver.executeCdpCommand("Network.setUserAgentOverride", map)
    }

    private fun webGlVendorOverride() {
        val webglVendor: String = if (args.containsKey(KEYS.WEB_GL_VENDOR.toString()) && args.getValue(KEYS.WEB_GL_VENDOR.toString()) != null)
            args.getValue(KEYS.WEB_GL_VENDOR.toString()) as String
        else "Intel Inc."

        val renderer = if (args.containsKey(KEYS.RENDERER.toString()) && args.getValue(KEYS.RENDERER.toString()) != null)
            args.getValue(KEYS.RENDERER.toString()) as String
        else "Intel Iris OpenGL Engine"

        evaluateOnNewDocument(WEB_GL_VENDOR, webglVendor, renderer)
    }

    private fun evaluateOnNewDocument(documentLocation: String, vararg args: Any?) {
        try {
            val res = javaClass.classLoader.getResource(documentLocation) ?: throw FileNotFoundException()

            val p: Path = Paths.get(res.toURI()).toAbsolutePath()

            val source = HashMap<String, Any>()
            source[SOURCE] = evaluationString(Files.readString(p), args)

            driver.executeCdpCommand(CDP_COMMAND, source.toMap())

        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun evaluationString(page: String, args: Array<out Any?>): String {
        val arguments = ArrayList<String>()
        args.forEachIndexed { _, any ->
            when (any) {
                null -> arguments.add("undefined")
                is Array<*> -> {
                    arguments.add(
                        any.joinToString(separator = ",", prefix = "[" , postfix = "]") { "\"${it}\""}
                    )
                }
                is String -> { arguments.add("\"$any\"") }
                else -> arguments.add(any.toString())
            }
        }
        return "($page)(${arguments.joinToString(separator = ",")})"
    }

    companion object {
        enum class KEYS {
            USER_AGENT,
            LANGUAGES,
            VENDOR,
            PLATFORM,
            WEB_GL_VENDOR,
            RENDERER,
            FIX_HAIRLINE,
            RUN_ON_IN_SECURE_ORIGINS
        }

        const val SOURCE = "source"

        const val WITH_UTILS = "js/utils.js"
        const val CHROME_APP = "js/chrome.app.js"
        const val CHROME_RUNTIME = "js/chrome.runtime.js"
        const val IFRAME_CONTENT_WINDOW = "js/iframe.contentWindow.js"
        const val MEDIA_CODECS = "js/media.codecs.js"
        const val NAVIGATOR_LANGUAGE = "js/navigator.languages.js"
        const val NAVIGATOR_PERMISSIONS = "js/navigator.permissions.js"
        const val NAVIGATOR_PLUGINS = "js/navigator.plugins.js"
        const val NAVIGATOR_VENDOR = "js/navigator.vendor.js"
        const val NAVIGATOR_WEB_DRIVER = "js/navigator.webdriver.js"
        const val WEB_GL_VENDOR = "js/webgl.vendor.js"
        const val WINDOW_OUTER_DIMENSION = "js/window.outerdimensions.js"
        const val HAIR_LINE_FIX = "js/hairline.fix.js"

        const val CDP_COMMAND = "Page.addScriptToEvaluateOnNewDocument"
    }
}