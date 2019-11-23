package defpackage.teleprogram

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.squareup.duktape.Duktape
import defpackage.teleprogram.api.Android
import defpackage.teleprogram.api.ApiEvaluator
import defpackage.teleprogram.api.Preferences
import defpackage.teleprogram.api.TeleClient
import defpackage.teleprogram.extensions.isRunning
import defpackage.teleprogram.extensions.pendingActivityFor
import defpackage.teleprogram.extensions.startForegroundService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import org.jetbrains.anko.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber

private typealias A = Android

@SuppressLint("InlinedApi")
class MainService : Service(), KodeinAware, CoroutineScope {

    private val parentKodein by closestKodein()

    override val kodein: Kodein by Kodein.lazy {

        extend(parentKodein)

        import(mainModule)
    }

    private val serviceJob = SupervisorJob()

    private val preferences: Preferences by instance()

    private val apiEvaluator: ApiEvaluator by instance()

    private val teleClient: TeleClient by instance()

    private val duktape: Duktape by instance()

    private lateinit var ticker: ReceiveChannel<Unit>

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @ObsoleteCoroutinesApi
    override fun onCreate() {
        super.onCreate()
        startForeground(
            Int.MAX_VALUE, NotificationCompat.Builder(applicationContext, "low")
                .setSmallIcon(R.drawable.ic_tv)
                .setContentTitle("Teleprogram watching")
                .setContentText("(ﾉ◕ヮ◕)ﾉ*:・ﾟ✧")
                .setContentIntent(pendingActivityFor<MainActivity>())
                .setOngoing(true)
                .setSound(null)
                .build()
        )
        acquireWakeLock()
        duktape.set("Android", A::class.java, ApiEvaluator(applicationContext))
        preferences.urlList?.lines()?.forEach {
            apiEvaluator.makeGetRequest()
        }
        ticker = ticker(200, 0)
        launch {
            for (event in ticker) {

            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock =
                powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name).apply {
                    acquire()
                }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            it.release()
            wakeLock = null
        }
    }

    override fun onDestroy() {
        ticker.cancel()
        serviceJob.cancelChildren()
        duktape.close()
        releaseWakeLock()
        super.onDestroy()
    }

    override val coroutineContext =
        Dispatchers.Main + serviceJob + CoroutineExceptionHandler { _, e ->
            Timber.e(e)
        }

    companion object {

        fun toggle(c: Context, run: Boolean, vararg params: Pair<String, Any?>): Boolean = c.run {
            return if (run) {
                try {
                    if (!activityManager.isRunning<MainService>()) {
                        startForegroundService<MainService>() != null
                    } else {
                        startService<MainService>(*params) != null
                    }
                } catch (e: Throwable) {
                    Timber.e(e)
                    toast(e.toString())
                    false
                }
            } else {
                if (activityManager.isRunning<MainService>()) {
                    stopService<MainService>()
                } else {
                    true
                }
            }
        }
    }
}