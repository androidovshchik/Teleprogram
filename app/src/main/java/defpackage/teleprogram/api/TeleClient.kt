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

private typealias OBJ = TdApi.Object

private typealias FUN = TdApi.Function

class TeleClient(context: Context) {

    val client: Client = Client.create({
        when (it.constructor) {
            TdApi.UpdateNewMessage.CONSTRUCTOR -> {

            }
        }
    }, null, null)

    val messages = LinkedBlockingQueue<TeleMessage>()

    init {
        sendSync<OK>(TdApi.SetLogVerbosityLevel(2))
        val tdLibParams = TdApi.TdlibParameters(
            false,
            context.getDatabasePath("app").parent,
            context.filesDir.absolutePath,
            true,
            true,
            true,
            true,
            492093,
            "95a11c4c658f568f7aeb0e8db4e12e12",
            ConfigurationCompat.getLocales(context.resources.configuration).get(0).language,
            Build.DEVICE,
            Build.VERSION.RELEASE,
            BuildConfig.VERSION_NAME,
            true,
            false
        )
        sendAsync<OK>(TdApi.SetTdlibParameters(tdLibParams)) { _ ->
            val key = "teleprogram".toByteArray()
            sendAsync<OK>(TdApi.SetDatabaseEncryptionKey(key)) { _ ->
                sendAsync<TdApi.AuthorizationState>(TdApi.GetAuthorizationState()) {

                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : OBJ> sendSync(query: FUN): T? {
        return Client.execute(query) as? T
    }

    fun sendAsync(query: FUN) {
        client.send(query, null)
    }

    inline fun <reified T : OBJ> sendAsync(query: FUN, crossinline block: (T?) -> Unit) {
        client.send(query) {
            block(it as? T)
        }
    }
}