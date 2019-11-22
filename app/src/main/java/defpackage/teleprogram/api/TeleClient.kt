package defpackage.teleprogram.api

import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi

class TeleClient {

    var client: Client? = null

    inline fun asasd() {
        client?.send(TdApi.CheckAuthenticationCode(it.getStringExtra("code"))) { obj ->

        }
    }
}