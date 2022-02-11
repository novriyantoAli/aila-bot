
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import io.javalin.Javalin
import kotlinx.coroutines.DelicateCoroutinesApi
import model.ActiveCredential
import model.Configuration
import ui.MainWindow
import util.copyDirectory
import util.deleteDirectory

import java.awt.EventQueue
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

        const val USER_DIR = "user.dir"
        const val OS_NAME = "os.name"

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
                app.get("/installation") { ctx ->
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

                        /**
                         * regenerate profile default
                         * 1. delete profile_default folder in system
                         * 2. delete profile_default folder in local
                         * 3. open browser with profile_default
                         * 4. copy profile default from system to local
                         */
                        // select configuration profile by os
                        config.configurationProfile.forEach {
                            if (it.os == "windows") {
                                // first step
                                val path = Paths.get((it.pathPrefix+System.getProperty(MainWindow.USER_NAME)+it.pathPostfix+it.profileDefault))
                                deleteDirectory(path.toFile())

                                // second step
                                val path2 = Paths.get((System.getProperty(USER_DIR)+"/"+it.profileDefault))
                                deleteDirectory(path2.toFile())

                                // third step
                                val arguments = arrayListOf<String>()
                                arguments.add(it.command.browser)
                                it.command.browserArguments.forEach { args ->
                                    if (args.contains("profile-directory"))
                                        arguments.add(args + it.profileDefault)
                                }

                                val process = Runtime.getRuntime().exec(arguments.toTypedArray())
                                process.waitFor()
//                                if(process.waitFor(5, TimeUnit.SECONDS)){
//                                    process.destroy()
//                                }

                                // fourth step
                                val fromSystemPath = it.pathPrefix + System.getProperty(MainWindow.USER_NAME) + it.pathPostfix + it.profileDefault
                                copyDirectory(Path.of(fromSystemPath), (System.getProperty(USER_DIR) + "/" + it.profileDefault))

//                                arguments.clear()
//
//                                arguments.add(it.command.killer)
//                                arguments.addAll(it.command.killerArguments)
//
//                                Runtime.getRuntime().exec(arguments.toTypedArray())
                            }
                        }
                    }
                    osName.contains(OS.LINUX.toString(), ignoreCase = true) -> {
                        System.setProperty(
                            WEB_DRIVER_PROPERTY_KEY,
                            System.getProperty(USER_DIR) + LINUX_EXTENSION
                        )
                        osSelected = OS.LINUX.toString().lowercase()

                        /**
                         * regenerate profile default
                         * 1. delete profile_default folder in system
                         * 2. delete profile_default folder in local
                         * 3. open browser with profile_default
                         * 4. copy profile default from system to local
                         */
                        // select configuration profile by os
                        config.configurationProfile.forEach {
                            if (it.os == "linux") {
                                // first step
                                val path = Paths.get((it.pathPrefix+System.getProperty(MainWindow.USER_NAME)+it.pathPostfix+it.profileDefault))
                                deleteDirectory(path.toFile())

                                // second step
                                val path2 = Paths.get((System.getProperty(USER_DIR)+"/"+it.profileDefault))
                                deleteDirectory(path2.toFile())

                                // third step
                                val arguments = arrayListOf<String>()
                                arguments.add(it.command.browser)
                                it.command.browserArguments.forEach { args ->
                                    if (args.contains("profile-directory"))
                                        arguments.add(args + it.profileDefault)
                                }
                                val process = Runtime.getRuntime().exec(arguments.toTypedArray())
                                process.waitFor()

//                                if (process.waitFor(2, TimeUnit.SECONDS)) {
//                                    process.destroy()
//                                }

                                // fourth step
                                val fromSystemPath = it.pathPrefix + System.getProperty(MainWindow.USER_NAME) + it.pathPostfix + it.profileDefault
                                copyDirectory(Path.of(fromSystemPath), (System.getProperty(USER_DIR)+ "/" + it.profileDefault))

//                                arguments.clear()
//
//                                arguments.add(it.command.killer)
//                                arguments.addAll(it.command.killerArguments)
//
//                                Runtime.getRuntime().exec(arguments.toTypedArray())
                            }
                        }
                    }
                }

                if (osSelected == null)
                    throw UnknownError("operation system not detected")

                EventQueue.invokeLater { MainWindow(APP_NAME, osSelected, config) }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}