package defpackage.teleprogram

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import defpackage.teleprogram.extensions.isRunning
import defpackage.teleprogram.extensions.startForegroundService
import org.jetbrains.anko.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber

@SuppressLint("InlinedApi")
class MainService : Service(), KodeinAware {

    private val parentKodein by closestKodein()

    override val kodein: Kodein by Kodein.lazy {

        extend(parentKodein)

        import(mainModule)
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            Int.MAX_VALUE, NotificationCompat.Builder(applicationContext, "low")
                .setSmallIcon(R.drawable.ic_tv)
                .setContentTitle("Teleprogram watch")
                .setContentText("(-, - )…zzzZZZ")
                .setOngoing(true)
                .setSound(null)
                .build()
        )
        acquireWakeLock()
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
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {

        fun toggleService(c: Context, run: Boolean, vararg p: Pair<String, Any?>): Boolean = c.run {
            return if (run) {
                try {
                    if (!activityManager.isRunning<MainService>()) {
                        startForegroundService<MainService>() != null
                    } else {
                        startService<MainService>(*p) != null
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