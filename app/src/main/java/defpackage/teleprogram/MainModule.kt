package defpackage.teleprogram

import android.app.Activity
import com.squareup.duktape.Duktape
import defpackage.teleprogram.api.ApiEvaluator
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.contexted
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

val mainModule = Kodein.Module("main") {

    bind<ApiEvaluator>() with provider {
        ApiEvaluator(instance())
    }

    bind<Duktape>() with provider {
        Duktape.create()
    }

    bind<PromptDialog>() with contexted<Activity>().provider {
        PromptDialog(context)
    }
}