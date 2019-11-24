package defpackage.teleprogram

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.os.ConfigurationCompat
import com.squareup.duktape.Duktape
import defpackage.teleprogram.api.Android
import defpackage.teleprogram.api.ApiEvaluator
import defpackage.teleprogram.api.FileManager
import defpackage.teleprogram.api.Preferences
import defpackage.teleprogram.extensions.isConnected
import defpackage.teleprogram.extensions.isRunning
import defpackage.teleprogram.extensions.pendingActivityFor
import defpackage.teleprogram.extensions.startForegroundService
import defpackage.teleprogram.model.TeleMessage
import kotlinx.coroutines.*
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import org.jetbrains.anko.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

private typealias A = Android

private typealias OK = TdApi.Ok

private typealias OBJ = TdApi.Object

private typealias FUN = TdApi.Function

@SuppressLint("InlinedApi")
class MainService : Service(), KodeinAware, CoroutineScope {

    private val parentKodein by closestKodein()

    override val kodein: Kodein by Kodein.lazy {

        extend(parentKodein)

        import(mainModule)
    }

    private val serviceJob = SupervisorJob()

    private val preferences: Preferences by instance()

    private val fileManager: FileManager by instance()

    private val duktape: Duktape by instance()

    private val apiEvaluator: ApiEvaluator by instance()

    //private lateinit var ticker: ReceiveChannel<Unit>

    private lateinit var wakeLock: PowerManager.WakeLock

    private val client: Client = Client.create({
        when (it.constructor) {
            TdApi.UpdateNewMessage.CONSTRUCTOR -> {

            }
        }
    }, null, null)

    private val messages = LinkedBlockingQueue<TeleMessage>()

    private val isAuthorized = AtomicBoolean(false)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("WakelockTimeout")
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
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name).apply {
            acquire()
        }
        duktape.set("Android", A::class.java, apiEvaluator)
        //ticker = ticker(200, 0)
        sendSync(TdApi.SetLogVerbosityLevel(2))
        sendAsync(TdApi.SetDatabaseEncryptionKey("teleprogram".toByteArray()))
        sendAsync(
            TdApi.SetTdlibParameters(
                TdApi.TdlibParameters(
                    false,
                    getDatabasePath("app").parent,
                    filesDir.absolutePath,
                    true,
                    true,
                    true,
                    true,
                    492093,
                    "95a11c4c658f568f7aeb0e8db4e12e12",
                    ConfigurationCompat.getLocales(resources.configuration).get(0).language,
                    Build.DEVICE,
                    Build.VERSION.RELEASE,
                    BuildConfig.VERSION_NAME,
                    true,
                    false
                )
            )
        )
        launch {
            withContext(Dispatchers.IO) {
                val scripts = arrayListOf<String>()
                if (connectivityManager.isConnected) {
                    fileManager.apply {
                        deleteFolder(scriptsDir)
                    }
                    preferences.listUrl?.lines()?.forEachIndexed { i, line ->
                        val url = line.trim()
                        if (url.isNotEmpty() && !url.startsWith("//")) {
                            apiEvaluator.makeGetRequest(url)?.let {
                                fileManager.saveFile(File(fileManager.scriptsDir, "$i.js"), it)
                                scripts.add(it)
                            }
                        }
                    }
                } else {
                    fileManager.apply {
                        scriptsDir.listFiles()?.forEach { file ->
                            readFile(file)?.let {
                                scripts.add(it)
                            }
                        }
                    }
                }
                if (scripts.isNotEmpty()) {
                    try {
                        duktape.evaluate(TextUtils.join(";", scripts))
                    } catch (e: Throwable) {
                        Timber.e(e)
                    }
                }
            }
            /*for (event in ticker) {

            }*/
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_CODE)) {
                sendAsync<OK>(TdApi.CheckAuthenticationCode(intent.getStringExtra(EXTRA_CODE))) {
                    isAuthorized.compareAndSet(false, it != null)
                    sendBroadcast(Intent(ACTION_TELEGRAM).apply {
                        putExtra(MainActivity.EXTRA_PROMPT, it != null)
                    })
                }
            }
            if (intent.hasExtra(EXTRA_MESSAGE)) {
                val message = intent.getSerializableExtra(EXTRA_MESSAGE) as TeleMessage
                messages.put(message)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        //ticker.cancel()
        serviceJob.cancelChildren()
        duktape.close()
        wakeLock.release()
        super.onDestroy()
    }

    private fun sendSync(query: FUN) {
        Client.execute(query)
    }

    private fun sendAsync(query: FUN) {
        client.send(query, null)
    }

    private inline fun <reified T : OBJ> sendAsync(query: FUN, crossinline block: (T?) -> Unit) {
        client.send(query) {
            block(it as? T)
        }
    }

    override val coroutineContext =
        Dispatchers.Main + serviceJob + CoroutineExceptionHandler { _, e ->
            Timber.e(e)
        }

    companion object {

        const val ACTION_TELEGRAM = "action_telegram"

        const val EXTRA_CODE = "extra_code"

        const val EXTRA_MESSAGE = "extra_message"

        fun toggle(context: Context, run: Boolean, vararg params: Pair<String, Any?>): Boolean {
            context.run {
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
}