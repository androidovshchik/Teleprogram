package defpackage.teleprogram.api

import android.content.Context
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class ApiEvaluator(context: Context) : KodeinAware {

    override val kodein by closestKodein(context)


}