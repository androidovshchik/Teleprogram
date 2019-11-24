@file:Suppress("unused", "DEPRECATION")

package defpackage.teleprogram.extension

import android.net.ConnectivityManager

val ConnectivityManager.isConnected: Boolean
    get() = activeNetworkInfo?.isConnected == true