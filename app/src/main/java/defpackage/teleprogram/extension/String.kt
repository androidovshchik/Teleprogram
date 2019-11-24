package defpackage.teleprogram.extension

import android.util.Base64

fun String.toBase64(): String {
    return Base64.encode(this.toByteArray(), Base64.NO_WRAP)
        .toString(Charsets.UTF_8)
}

fun String.fromBase64(): String {
    return Base64.decode(this, Base64.NO_WRAP)
        .toString(Charsets.UTF_8)
}