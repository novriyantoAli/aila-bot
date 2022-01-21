import com.github.cliftonlabs.json_simple.JsonArray
import com.github.cliftonlabs.json_simple.JsonObject
import com.github.cliftonlabs.json_simple.Jsoner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.Reader
import java.nio.file.Paths


class Launcher {
    companion object {
        private fun execute(keyword: String, channel: String, playlistName: String, playlist: String, playlistHome: String, location: String, profile: String) {
            val youtube = YoutubePlayer( playlistName = playlistName, playlistHome=playlistHome, channel = channel, playlistWatch = playlist, profile = location + profile)
            try {
                youtube.searchAndGoFirst(keyword)
                while (true) {
                    youtube.checkAd()

                    if (youtube.hasAd)
                        youtube.skipAd()

                    if (!youtube.hasAd && !youtube.hasMinimalQuality) {
                        youtube.changeResolution()
                    }

                    if (youtube.checkPlaylistEnd(playlistName) == null) {
                        youtube.clearCookie()

                        Thread.sleep(7000)

                        youtube.goToHomePage()
                        youtube.searchAndGoFirst(keyword)
                    }

                    Thread.sleep(5000)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val osName = System.getProperty("os.name")
                val pathDriver = Paths.get("").toAbsolutePath().toString()
                println(osName)
                when {
                    osName.contains("windows", ignoreCase = true) -> {
                        val windowsExt = "//chromedriver.exe"
                        System.setProperty("webdriver.chrome.driver", pathDriver + windowsExt);
                    }
                    osName.contains("linux", ignoreCase = true) -> {
                        val linuxExt = "/chromedriver"
                        System.setProperty("webdriver.chrome.driver", pathDriver + linuxExt)
                    }
                }

                val path = (Paths.get("").toAbsolutePath().toString()+"/config.json")
                val selectedFile = File(path)
                    val reader: Reader = selectedFile.bufferedReader()
                    // create parser
                    val parser = Jsoner.deserialize(reader) as JsonObject

                    val ky = parser["kata_kunci"] as String
                    val pln = parser["nama_playlist"] as String
                    val ch = parser["kode_channel"] as String
                    val pl = parser["kode_playlist"] as String
                    val hpl = parser["kode_halaman_playlist"] as String
                    val location = parser["tempat_profile"] as String

                    val profiles = parser["profile"] as JsonArray
                    runBlocking {
                        withContext(coroutineContext) {
                            for ((index, item) in profiles.withIndex()) {
                                async(Dispatchers.IO) { execute(
                                    keyword=ky,channel=ch,playlist=pl,playlistHome=hpl,location=location,profile=(item as String),playlistName=pln
                                ) }
                            }
                        }
                    }
            } catch (e: IOException) { e.printStackTrace() }
        }
    }
}