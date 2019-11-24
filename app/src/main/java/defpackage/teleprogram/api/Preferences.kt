package defpackage.teleprogram.api

import android.content.Context
import com.chibatching.kotpref.KotprefModel

class Preferences(context: Context) : KotprefModel(context) {

    override val kotprefName: String = "${context.packageName}_preferences"

    var appId by nullableStringPref(null, "app_id")

    var telephone by nullableStringPref(null, "telephone")

    var baseUrl by nullableStringPref(null, "base_url")

    var mainUrl by nullableStringPref(null, "main_url")

    var runApp by booleanPref(false, "run_app")

    init {
        if (appId == null) {
            appId = (1000..9999).random().toString()
        }
    }
}