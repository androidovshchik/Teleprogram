package defpackage.teleprogram.api

import android.content.Context
import java.io.File

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
}
