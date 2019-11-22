package defpackage.teleprogram.api

import android.content.Context
import com.couchbase.lite.Database
import okhttp3.OkHttpClient
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

class ApiEvaluator(context: Context) : KodeinAware, Android {

    override val kodein by closestKodein(context)

    private val teleClient: TeleClient by instance()

    private val okHttpClient: OkHttpClient by instance()

    private val database: Database by instance()
}