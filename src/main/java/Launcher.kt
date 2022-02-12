
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import io.javalin.Javalin
import kotlinx.coroutines.DelicateCoroutinesApi
import model.ActiveCredential
import model.Configuration
import model.ConfigurationProfile
import ui.MainWindow
import util.checkEmptyDirectory
import util.copyDirectory
import util.deleteDirectory

import java.awt.EventQueue
import java.io.File
import java.io.FileReader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class Launcher {
    companion object {

        enum class OS { WINDOWS, LINUX }

        private const val WEB_DRIVER_PROPERTY_KEY = "webdriver.chrome.driver"

        private const val FILE_CONFIG_NAME = "/config.json"
        private const val FILE_CREDENTIALS_NAME = "credential.json"

        private const val WINDOWS_EXTENSION = "//chromedriver.exe"
        private const val LINUX_EXTENSION = "/chromedriver"
        private const val URL_INSTALLATION = "/installation"

        const val USER_DIR = "user.dir"
        private const val OS_NAME = "os.name"

        private const val APP_NAME = "Aila BOT"

        @DelicateCoroutinesApi
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val reader = JsonReader(FileReader((System.getProperty(USER_DIR) + FILE_CONFIG_NAME)))
                val config: Configuration = Gson().fromJson(reader, Configuration::class.java)

                val osName = System.getProperty(OS_NAME)
                var osSelected: String? = null

                val app = Javalin.create().start(config.port)
                app.get(URL_INSTALLATION) { ctx ->
                    val readerActiveCredentials = JsonReader(FileReader(FILE_CREDENTIALS_NAME))
                    val activeCredentials = Gson().fromJson<ActiveCredential>(
                        readerActiveCredentials, ActiveCredential::class.java
                    )

                    ctx.json(activeCredentials)
                }

                when {
                    osName.contains(OS.WINDOWS.toString(), ignoreCase = true) -> {
                        System.setProperty(
                            WEB_DRIVER_PROPERTY_KEY,
                            System.getProperty(USER_DIR) + WINDOWS_EXTENSION
                        )
                        osSelected = OS.WINDOWS.toString().lowercase()
                    }
                    osName.contains(OS.LINUX.toString(), ignoreCase = true) -> {
                        System.setProperty(
                            WEB_DRIVER_PROPERTY_KEY,
                            System.getProperty(USER_DIR) + LINUX_EXTENSION
                        )
                        osSelected = OS.LINUX.toString().lowercase()
                    }
                }

                if (osSelected == null)
                    throw UnknownError("operation system not detected")

                val directoryRunning = File((System.getProperty(USER_DIR) + config.runningDirectory.substring(0, (config.runningDirectory.length-1))))
                if (!directoryRunning.exists() || !directoryRunning.isDirectory) {
                    directoryRunning.mkdir()
                }

                /**
                 * NEW ALGORITHM
                 * just for your information, fresh install chrome its recommended
                 * 1. check if we have directory default
                 * 2. if don't have directory default, open chrome using params --user-data-dir=default
                 * 3. if we have default, don't open chrome
                 */
                if (checkEmptyDirectory((System.getProperty(USER_DIR) + config.defaultDirectory))) {
                    // open chrome
                    for (i in config.configurationProfile) {
                        if (i.os == osSelected) {
                            val arguments = arrayListOf<String>()
                            arguments.add(i.command.browser)
                            i.command.browserArguments.forEach { ar ->
                                if (ar.contains("profile-directory"))
                                    arguments.add(ar + config.defaultDirectory.substring(1, config.defaultDirectory.length))
                                else if (ar.contains("user-data-dir"))
                                    arguments.add(ar + System.getProperty(USER_DIR) + config.defaultDirectory)
                                else
                                    arguments.add(ar)
                            }

                            val process = Runtime.getRuntime().exec(arguments.toTypedArray())
                            process.waitFor()
                            break
                        }
                    }
                }

                EventQueue.invokeLater { MainWindow(APP_NAME, osSelected, config) }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}