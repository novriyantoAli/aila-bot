package util

import com.google.gson.Gson
import model.ActiveCredential
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * true for empty
 * false for not
 */
fun checkEmptyDirectory(location: String): Boolean {
    val directory = File(location)
    if (directory.isDirectory) {
        val files = directory.list()
        return directory.length() <= 0
    }

    return true
}

fun deleteDirectory(directoryToBeDeleted: File): Boolean {
    val allContents = directoryToBeDeleted.listFiles()
    if (allContents != null) {
        for (file in allContents)
            deleteDirectory(file)
    }
    return directoryToBeDeleted.delete()
}

fun writeCredentials(username: String? = null, password: String? = null){
    val activeCredentials = ActiveCredential(username.toString(), password.toString())

    val writer: Writer = FileWriter("credential.json")

    Gson().toJson(activeCredentials, writer)

    writer.close()
}

fun copyDirectory(fromPath: Path, destinationPath: String){
    Files.walk(fromPath).forEach { source: Path -> copySourceToDest(fromPath, source, destinationPath) }
}

private fun copySourceToDest(fromPath: Path, source: Path, dstPath: String) {
    val destination = Paths.get(dstPath, source.toString().substring(fromPath.toString().length))
    try {
        Files.copy(source, destination)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}