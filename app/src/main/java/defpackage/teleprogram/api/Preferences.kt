package defpackage.teleprogram.api

import android.content.Context
import com.chibatching.kotpref.KotprefModel

class Preferences(context: Context) : KotprefModel(context) {

    override val kotprefName: String = "${context.packageName}_preferences"

    var appId by nullableStringPref(null, "app_id")

    var telephone by nullableStringPref(null, "telephone")

    var listUrl by nullableStringPref(null, "url_list")

    var runApp by booleanPref(false, "run_app")

    var lastLaunch by longPref(0L, "last_launch")

    init {
        if (appId == null) {
            appId = (1000..9999).random().toString()
        }
    }
}