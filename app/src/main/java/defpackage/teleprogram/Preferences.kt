package defpackage.teleprogram

import android.content.Context
import com.chibatching.kotpref.KotprefModel

class Preferences(context: Context) : KotprefModel(context) {

    override val kotprefName: String = "${context.packageName}_preferences"

    var runApp by booleanPref(false, "run_app")

    var telegramPhone by nullableStringPref(null, "telegram_phone")

    var lastLaunch by longPref(0L, "last_launch")
}