@file:Suppress("unused")

package defpackage.teleprogram.extensions

import android.view.View

inline fun <R> View.lock(block: (View) -> R): R {
    isEnabled = false
    try {
        return block(this)
    } finally {
        isEnabled = true
    }
}