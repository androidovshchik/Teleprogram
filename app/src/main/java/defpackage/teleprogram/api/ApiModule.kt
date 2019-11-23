package defpackage.teleprogram.api

import android.content.Context
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration
import defpackage.teleprogram.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.kodein.di.Kodein
import org.kodein.di.generic.*
import timber.log.Timber

val apiModule = Kodein.Module("api") {

    bind<Preferences>() with provider {
        Preferences(instance())
    }

    bind<CronManager>() with provider {
        CronManager(instance())
    }

    bind<FileManager>() with provider {
        FileManager(instance())
    }

    bind<TeleClient>() with eagerSingleton {
        TeleClient(instance())
    }

    bind<OkHttpClient>() with singleton {
        OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message ->
                    Timber.tag("NETWORK")
                        .d(message)
                }).apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }.build()
    }

    bind<Database>() with singleton {
        Database("app", DatabaseConfiguration(instance() as Context))
    }
}