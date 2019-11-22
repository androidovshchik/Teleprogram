package defpackage.teleprogram

import android.app.Application
import android.content.Context
import defpackage.teleprogram.api.apiModule
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.bind
import org.kodein.di.generic.provider

@Suppress("unused")
class MainApp : Application(), KodeinAware {

    override val kodein by Kodein.lazy {

        bind<Context>() with provider {
            applicationContext
        }

        import(apiModule)
    }

    override fun onCreate() {
        super.onCreate()
    }
}