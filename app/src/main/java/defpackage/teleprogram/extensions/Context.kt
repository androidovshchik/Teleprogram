@file:Suppress("unused")

package defpackage.teleprogram.extensions

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startService

tailrec fun Context?.activity(): Activity? = when (this) {
    is Activity -> this
    else -> (this as? ContextWrapper)?.baseContext?.activity()
}

fun Context.areGranted(vararg permissions: String): Boolean {
    for (permission in permissions) {
        if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}

inline fun <reified T : Service> Context.startForegroundService(vararg params: Pair<String, Any?>): ComponentName? {
    return if (isOreoPlus()) {
        startForegroundService(intentFor<T>(*params))
    } else {
        startService<T>(*params)
    }
}