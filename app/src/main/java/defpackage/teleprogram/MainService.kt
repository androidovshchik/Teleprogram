package defpackage.teleprogram

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.squareup.duktape.Duktape
import defpackage.teleprogram.api.*
import defpackage.teleprogram.extensions.isConnected
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
import java.io.File

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

    private val fileManager: FileManager by instance()

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
        ticker = ticker(200, 0)
        launch {
            withContext(Dispatchers.IO) {
                val scripts = arrayListOf<String>()
                if (connectivityManager.isConnected) {
                    fileManager.deleteFolder(fileManager.scriptsDir)
                    preferences.urlList?.lines()?.forEachIndexed { i, line ->
                        val url = line.trim()
                        if (url.isEmpty() || url.startsWith("//")) {
                            return@forEachIndexed
                        }
                        apiEvaluator.makeGetRequest(url)?.let { script ->
                            fileManager.saveFile(File(fileManager.scriptsDir, "$i.js"), script)
                            scripts.add(script)
                        }
                    }
                } else {
                    fileManager.apply {
                        scriptsDir.listFiles()?.forEach {
                            readFile(it)?.let { script ->
                                scripts.add(script)
                            }
                        }
                    }
                }
                if (scripts.isNotEmpty()) {
                    duktape.apply {
                        set("Android", A::class.java, apiEvaluator)
                        evaluate(TextUtils.join(";", scripts))
                    }
                }
            }
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
                    if (activityManager.isRunning<MainService>()) {
                        startService<MainService>(*params) != null
                    } else {
                        startForegroundService<MainService>() != null
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