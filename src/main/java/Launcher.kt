import kotlinx.coroutines.DelicateCoroutinesApi
import java.awt.EventQueue
import java.nio.file.Paths
import kotlin.system.exitProcess


class Launcher {
    companion object {
//        @OptIn(DelicateCoroutinesApi::class)
        @DelicateCoroutinesApi
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val osName = System.getProperty("os.name")
                val pathDriver = Paths.get("").toAbsolutePath().toString()
                when {
                    osName.contains("windows", ignoreCase = true) -> {
                        val windowsExt = "//chromedriver.exe"
                        System.setProperty("webdriver.chrome.driver", pathDriver + windowsExt)
                    }
                    osName.contains("linux", ignoreCase = true) -> {
                        val linuxExt = "/chromedriver"
                        System.setProperty("webdriver.chrome.driver", pathDriver + linuxExt)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                exitProcess(0)
            }
            /* Create and display the form */
            EventQueue.invokeLater { MainWindow("Aila Bot") }
//            try {
//                val osName = System.getProperty("os.name")
//                val pathDriver = Paths.get("").toAbsolutePath().toString()
//                println(osName)
//                when {
//                    osName.contains("windows", ignoreCase = true) -> {
//                        val windowsExt = "//chromedriver.exe"
//                        System.setProperty("webdriver.chrome.driver", pathDriver + windowsExt)
//                    }
//                    osName.contains("linux", ignoreCase = true) -> {
//                        val linuxExt = "/chromedriver"
//                        System.setProperty("webdriver.chrome.driver", pathDriver + linuxExt)
//                    }
//                }
//
//                val path = (Paths.get("").toAbsolutePath().toString()+"/config.json")
//                val selectedFile = File(path)
//                    val reader: Reader = selectedFile.bufferedReader()
//                    // create parser
//                    val parser = Jsoner.deserialize(reader) as JsonObject
//
//                    val ky = parser["kata_kunci"] as String
//                    val pln = parser["nama_playlist"] as String
//                    val ch = parser["kode_channel"] as String
//                    val pl = parser["kode_playlist"] as String
//                    val hpl = parser["kode_halaman_playlist"] as String
//                    val location = parser["tempat_profile"] as String
//
//                    val profiles = parser["profile"] as JsonArray
//                    runBlocking {
//                        withContext(coroutineContext) {
//                            for ((index, item) in profiles.withIndex()) {
//                                async(Dispatchers.IO) { execute(
//                                    keyword=ky,channel=ch,playlist=pl,playlistHome=hpl,location=location,profile=(item as String),playlistName=pln
//                                ) }
//                            }
//                        }
//                    }
//            } catch (e: IOException) { e.printStackTrace() }
        }
    }
}