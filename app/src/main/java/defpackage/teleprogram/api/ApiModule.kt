package defpackage.teleprogram.api

import defpackage.teleprogram.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import timber.log.Timber

val apiModule = Kodein.Module("api") {

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
}