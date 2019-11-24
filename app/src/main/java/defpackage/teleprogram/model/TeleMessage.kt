package defpackage.teleprogram.model

import java.io.Serializable

class TeleMessage(
    val chatId: Long,
    val text: String
) : Serializable