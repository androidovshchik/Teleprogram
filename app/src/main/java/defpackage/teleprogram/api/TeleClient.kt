package defpackage.teleprogram.api

import android.content.Context
import android.os.Build
import androidx.core.os.ConfigurationCompat
import defpackage.teleprogram.BuildConfig
import defpackage.teleprogram.model.TeleMessage
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.LinkedBlockingQueue

private typealias OK = TdApi.Ok

class TeleClient(context: Context) {

    val client: Client = Client.create({
        when (it.constructor) {
            TdApi.UpdateNewMessage.CONSTRUCTOR -> {

            }
        }
    }, null, null)

    val messages = LinkedBlockingQueue<TeleMessage>()

    init {
        val tdLibParams = TdApi.TdlibParameters().apply {
            apiId = 492093
            apiHash = "95a11c4c658f568f7aeb0e8db4e12e12"
            databaseDirectory = context.cacheDir.absolutePath
            filesDirectory = context.filesDir.absolutePath
            applicationVersion = BuildConfig.VERSION_NAME
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            systemLanguageCode =
                ConfigurationCompat.getLocales(context.resources.configuration).get(0).language
        }
        sendSync<OK>(TdApi.SetLogVerbosityLevel(2))
        sendAsync<OK>(TdApi.SetTdlibParameters(tdLibParams)) { _ ->
            val key = "teleprogram".toByteArray()
            sendAsync<OK>(TdApi.SetDatabaseEncryptionKey(key)) { _ ->

            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> sendSync(query: TdApi.Function): T? {
        return Client.execute(query) as? T
    }

    fun sendAsync(query: TdApi.Function) {
        client.send(query, null)
    }

    inline fun <reified T> sendAsync(query: TdApi.Function, crossinline block: (T?) -> Unit) {
        client.send(query) {
            block(it as? T)
        }
    }
}