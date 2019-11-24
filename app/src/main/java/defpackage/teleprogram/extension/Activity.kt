@file:Suppress("unused")

package defpackage.teleprogram.extension

import android.app.Activity

fun Activity.requestPermissions(requestCode: Int, vararg permissions: String) {
    if (isMarshmallowPlus()) {
        requestPermissions(permissions, requestCode)
    }
}

inline fun <reified T> Activity?.makeCallback(action: T.() -> Unit) {
    if (this is T && !isFinishing) {
        action()
    }
}