package defpackage.teleprogram.api

import android.content.Context
import androidx.annotation.Keep
import com.couchbase.lite.Database
import defpackage.teleprogram.model.TeleMessage
import okhttp3.OkHttpClient
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

@Keep
interface Android {

    fun sendTeleMessage(message: String)

    fun makeGetRequest(message: String)
}

class ApiEvaluator(context: Context) : KodeinAware, Android {

    override val kodein by closestKodein(context)

    private val teleClient: TeleClient by instance()

    private val okHttpClient: OkHttpClient by instance()

    private val database: Database by instance()

    override fun sendTeleMessage(message: String) {
        teleClient.messages.add(TeleMessage())
    }

    override fun makeGetRequest(message: String) {
        okHttpClient
    }
}