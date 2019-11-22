package defpackage.teleprogram

import com.squareup.duktape.Duktape
import defpackage.teleprogram.api.CronManager
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

val mainModule = Kodein.Module("main") {

    bind<CronManager>() with provider {
        CronManager(instance())
    }

    bind<Duktape>() with provider {
        Duktape.create()
    }
}