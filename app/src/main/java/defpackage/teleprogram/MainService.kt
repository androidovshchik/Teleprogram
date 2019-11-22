package defpackage.teleprogram

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import org.jetbrains.anko.*
import timber.log.Timber
import java.net.URL
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("InlinedApi")
class MainService : Service() {

    private lateinit var preferences: Preferences

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
            100, NotificationCompat.Builder(applicationContext, "low")
                .setSmallIcon(R.drawable.ic_send_white_24dp)
                .setContentTitle("Фоновая работа с Telegram")
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

    private fun initClient() {
        client = Client.create({
            when (it.constructor) {
                TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                    onNextMessage((it as TdApi.UpdateNewMessage).message)
                }
            }
        }, null, null)
        val tdLibParams = TdApi.TdlibParameters().apply {
            apiId = 913999
            apiHash = "ce68cff091f15a9c559c783c40a432d1"
            databaseDirectory = cacheDir.absolutePath
            filesDirectory = filesDir.absolutePath
            applicationVersion = BuildConfig.VERSION_NAME
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            systemLanguageCode = Locale.getDefault().language
        }
        client?.send(TdApi.SetTdlibParameters(tdLibParams)) { _ ->
            client?.send(TdApi.SetDatabaseEncryptionKey("telegram_sms".toByteArray())) { _ ->
                Client.execute(TdApi.SetLogVerbosityLevel(2))
                client?.send(TdApi.GetProxies()) {
                    val url = try {
                        URL("http://${preferences.proxyUrl}")
                    } catch (e: Throwable) {
                        Timber.e(e)
                        null
                    }
                    if (it is TdApi.Proxies && url != null) {
                        if (preferences.proxyType > 0) {
                            val (user, password) = url.userInfo?.split(":") ?: listOf("", "")
                            val type = when (preferences.proxyType) {
                                1 -> TdApi.ProxyTypeSocks5(user, password)
                                2 -> TdApi.ProxyTypeHttp(user, password, false)
                                else -> TdApi.ProxyTypeHttp(user, password, true)
                            }
                            if (it.proxies.isEmpty()) {
                                client?.send(TdApi.AddProxy(url.host, url.port, true, type), null)
                            } else {
                                client?.send(
                                    TdApi.EditProxy(
                                        it.proxies[0].id,
                                        url.host,
                                        url.port,
                                        true,
                                        type
                                    ), null
                                )
                            }
                        } else if (it.proxies.isNotEmpty()) {
                            client?.send(TdApi.RemoveProxy(it.proxies[0].id), null)
                        }
                    }
                }
                client?.send(TdApi.GetAuthorizationState()) { obj ->
                    when (obj.constructor) {
                        TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                            client?.send(
                                TdApi.SetAuthenticationPhoneNumber(
                                    preferences.phone,
                                    TdApi.PhoneNumberAuthenticationSettings(false, false, false)
                                ), null
                            )
                            sendBroadcast(Intent("TGM_PROMPT").apply {
                                putExtra("prompted", false)
                            })
                        }
                        TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                            client?.send(
                                TdApi.SendPhoneNumberVerificationCode(
                                    preferences.phone,
                                    TdApi.PhoneNumberAuthenticationSettings(false, false, false)
                                ), null
                            )
                            sendBroadcast(Intent("TGM_PROMPT").apply {
                                putExtra("prompted", false)
                            })
                        }
                        // not all cases matches correct here e.g. TdApi.AuthorizationStateWaitPassword
                        else -> {
                            Timber.i(obj.toString())
                            client?.send(TdApi.GetChats(Long.MAX_VALUE, 0, MAX_DIALOGS)) {
                                if (it is TdApi.Chats) {
                                    isAuthorized.compareAndSet(false, true)
                                    bgToast("Вы успешно авторизованы в телеграм")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onNextMessage(message: TdApi.Message) {
        if (message.content !is TdApi.MessageText) {
            return
        }
        val text = (message.content as TdApi.MessageText).text.text
            .replace(newLineRegex, " ")
        SourceType.fromText(text)?.let { arg1 ->
            Timber.i("SourceType.fromText: $arg1")
            tryCast<Argument<ContentType>>(arg1.param) { arg2 ->
                tryCast<Argument<MoneyType>>(arg2.param) { arg3 ->
                    db.moneyDao().insert(TMoney().apply {
                        source = arg1.enum.id
                        content = arg2.enum.id
                        money = arg3.enum.id
                        amount = arg3.param as Float
                        chat = message.chatId
                        timestamp = message.date * 1000L
                        offset = offsetTimeMillis()
                        datetime = formatter.format(message.date * 1000L)
                    })
                    client?.send(TdApi.GetChat(message.chatId)) {
                        if (it is TdApi.Chat) {
                            db.chatDao().upsert(TDialog().apply {
                                chat = it.id
                                title = it.title
                            })
                        }
                    }
                }
            }
            return
        }
        if (message.date < startSeconds) {
            return
        }
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