package defpackage.teleprogram

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import defpackage.teleprogram.api.Preferences

class RebootReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = Preferences(context)
        MainService.toggle(context, preferences.runApp)
    }
}