package defpackage.teleprogram

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import defpackage.teleprogram.api.apiModule
import defpackage.teleprogram.extensions.isOreoPlus
import org.jetbrains.anko.notificationManager
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.bind
import org.kodein.di.generic.provider
import timber.log.Timber

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
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        if (isOreoPlus()) {
            notificationManager.createNotificationChannel(
                NotificationChannel("low", "Low", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }
}