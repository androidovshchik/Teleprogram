package defpackage.teleprogram

import android.content.Context
import com.chibatching.kotpref.KotprefModel

class Preferences(context: Context) : KotprefModel(context) {

    override val kotprefName: String = "${context.packageName}_preferences"

    var runApp by booleanPref(false, "run_app")

    var phone by nullableStringPref(null, "phone")

    var lastTime by longPref(0L, "last_time")
}