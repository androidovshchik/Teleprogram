@file:Suppress("unused")

package defpackage.teleprogram.extensions

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.content.*
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

inline fun <reified T : Activity> Context.pendingActivityFor(
    flags: Int = PendingIntent.FLAG_UPDATE_CURRENT,
    vararg params: Pair<String, Any?>
): PendingIntent =
    PendingIntent.getActivity(applicationContext, 0, intentFor<T>(*params), flags)

inline fun <reified T : BroadcastReceiver> Context.pendingReceiverFor(
    flags: Int = PendingIntent.FLAG_UPDATE_CURRENT,
    vararg params: Pair<String, Any?>
): PendingIntent =
    PendingIntent.getBroadcast(applicationContext, 0, intentFor<T>(*params), flags)

inline fun <reified T : BroadcastReceiver> Context.pendingReceiverFor(
    action: String,
    flags: Int = PendingIntent.FLAG_UPDATE_CURRENT
): PendingIntent =
    PendingIntent.getBroadcast(applicationContext, 0, Intent(action), flags)