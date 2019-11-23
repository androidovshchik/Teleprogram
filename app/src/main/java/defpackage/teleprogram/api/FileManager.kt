package defpackage.teleprogram.api

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

@Suppress("unused")
class FileManager(context: Context) {

    val externalDir = context.getExternalFilesDir(null)?.apply {
        mkdirs()
    }

    val internalDir: File = context.filesDir.apply {
        mkdirs()
    }

    val scriptsDir = File(externalDir, "scripts").apply {
        mkdirs()
    }

    val appletsDir = File(externalDir, "applets").apply {
        mkdirs()
    }

    fun saveFile(file: File, text: String) {
        try {
            FileOutputStream(file).use {
                it.write(text.toByteArray())
                it.flush()
            }
        } catch (e: Throwable) {
            Timber.e(e)
            file.delete()
        }
    }

    fun readFile(file: File): String? {
        return try {
            return file.bufferedReader().use {
                it.readText()
            }
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }
    }

    fun deleteFolder(file: File) {
        file.apply {
            if (exists()) {
                listFiles()?.forEach {
                    if (it.isDirectory) {
                        deleteFolder(it)
                    }
                    it.delete()
                }
            }
        }
    }
}
