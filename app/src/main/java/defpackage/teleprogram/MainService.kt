package defpackage.teleprogram

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import defpackage.teleprogram.api.Preferences
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import org.jetbrains.anko.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("InlinedApi")
class MainService : Service(), KodeinAware {

    override val kodein by closestKodein()

    private var wakeLock: PowerManager.WakeLock? = null

    private val startSeconds = currentTimeMillis(false) / 1000

    private var isAuthorized = AtomicBoolean(false)

    var client: Client? = null

    val subscriptions = arrayListOf<TSubscription>()

    val messages = LinkedBlockingQueue<BotMessage>()

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
        Timber.i("Current time offset: ${offsetTimeMillis()}")
        device = Settings.Secure.getString(contentResolver, "bluetooth_name")
        preferences = Preferences(applicationContext)
        subscriptionFuture = doAsync {
            synchronized(subscriptions) {
                subscriptions.apply {
                    clear()
                    addAll(db.subscriptionDao().getAll())
                }
            }
        }
        initClient()
        disposable.add(
            Observable.interval(200, TimeUnit.MILLISECONDS, Schedulers.io())
                .subscribe({
                    if (!isAuthorized.get()) {
                        return@subscribe
                    }
                    messages.poll()?.let {
                        val entities = if (it.hasMarkdown) {
                            val result = Client.execute(
                                TdApi.ParseTextEntities(
                                    it.text,
                                    TdApi.TextParseModeMarkdown()
                                )
                            )
                            if (result is TdApi.FormattedText) {
                                result.entities.apply {
                                    // markdown fix
                                    forEach { entity ->
                                        entity.length += 7
                                    }
                                }
                            } else {
                                Timber.e(result.toString())
                                it.showLog = true
                                arrayOf()
                            }
                        } else arrayOf()
                        val message = TdApi.InputMessageText(
                            TdApi.FormattedText(it.text, entities),
                            true,
                            true
                        )
                        client?.send(
                            TdApi.SendMessage(it.chatId, 0, true, true, null, message)
                        ) { obj ->
                            if (it.showLog) {
                                Timber.i(obj.toString())
                            }
                        }
                    }
                }, {
                    Timber.e(it)
                })
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra("code")) {
                client?.send(TdApi.CheckAuthenticationCode(it.getStringExtra("code"))) { obj ->
                    Timber.d(obj.toString())
                    isAuthorized.compareAndSet(false, obj.constructor == TdApi.Ok.CONSTRUCTOR)
                    sendBroadcast(Intent("TGM_PROMPT").apply {
                        putExtra("prompted", obj.constructor == TdApi.Ok.CONSTRUCTOR)
                    })
                    bgToast(
                        if (obj.constructor == TdApi.Ok.CONSTRUCTOR) {
                            "Вы успешно авторизовались в телеграм"
                        } else {
                            "Неверный код"
                        }
                    )
                }
            }
        }
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
        client?.close()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {

        const val MAX_DIALOGS = 1000

        /**
         * @return true if service is running
         */
        @Throws(SecurityException::class)
        fun start(context: Context, vararg params: Pair<String, Any?>): Boolean = context.run {
            return if (!activityManager.isRunning<MainService>()) {
                startForegroundService<MainService>() != null
            } else {
                startService<MainService>(*params) != null
            }
        }

        /**
         * @return true if service is stopped
         */
        fun stop(context: Context): Boolean = context.run {
            if (activityManager.isRunning<MainService>()) {
                return stopService<MainService>()
            }
            return true
        }
    }
}