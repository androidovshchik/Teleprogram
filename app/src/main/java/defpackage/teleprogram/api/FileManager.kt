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

    fun saveFile(file: File, text: String) {
        try {
            FileOutputStream(file).use {
                it.write(text.toByteArray())
            }
        } catch (e: Throwable) {
            Timber.e(e)
            file.delete()
        }
    }
}
