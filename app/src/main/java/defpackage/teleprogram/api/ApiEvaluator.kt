package defpackage.teleprogram.api

import android.content.Context
import androidx.annotation.Keep
import com.couchbase.lite.Database
import defpackage.teleprogram.MainService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.lang.ref.WeakReference

@Keep
interface Android {

    fun getAppId(): String

    fun registerApplet()

    fun sendTeleMessage(message: String)

    fun makeGetRequest(url: String): String?
}

class ApiEvaluator(context: Context) : KodeinAware, Android {

    override val kodein by closestKodein(context)

    private val reference = WeakReference(context)

    private val preferences: Preferences by instance()

    private val okHttpClient: OkHttpClient by instance()

    private val database: Database by instance()

    override fun getAppId(): String {
        return preferences.appId.toString()
    }

    override fun registerApplet() {

    }

    override fun sendTeleMessage(message: String) {
        reference.get()?.let {
            MainService.toggle(it, true)
        }
    }

    override fun makeGetRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .build()
        return try {
            okHttpClient.newCall(request).execute().body()?.string()
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }
    }
}