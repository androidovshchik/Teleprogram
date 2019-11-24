@file:Suppress("unused", "DEPRECATION")

package defpackage.teleprogram.extension

import android.app.ActivityManager
import android.app.Service

inline fun <reified T : Service> ActivityManager.isRunning(): Boolean {
    for (service in getRunningServices(Integer.MAX_VALUE)) {
        if (T::class.java.name == service.service.className) {
            return true
        }
    }
    return false
}