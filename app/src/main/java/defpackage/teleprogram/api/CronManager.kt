package defpackage.teleprogram.api

import android.content.Context
import androidx.work.*
import com.squareup.duktape.Duktape
import defpackage.teleprogram.extension.toBase64
import org.kodein.di.KodeinAware

class ApiWorker(context: Context, params: WorkerParameters) : Worker(context, params), KodeinAware {

    override fun doWork(): Result {
        tags
        val retry = inputData.getBoolean(PARAM_RETRY, false)
        val javascript = inputData.getString(PARAM_JAVASCRIPT)
        Duktape.create().use {
            it.set("Android", Android::class.java, ApiEvaluator(applicationContext))
            when {
                it.evaluate(javascript) == RESULT_OK -> Result.success()
                retry -> Result.retry()
                else -> Result.failure()
            }
        }
    }

    override fun onStopped() {
        super.onStopped()
    }

    companion object {

        const val PARAM_RETRY = "retry"

        const val PARAM_JAVASCRIPT = "javascript"

        private const val RESULT_OK = 0
    }
}

class CronManager(context: Context) {

    val workManager = WorkManager.getInstance(context)

    fun launch(type: Int, url: String) {
        val request = OneTimeWorkRequestBuilder<ApiWorker>()
            .setInputData(
                Data.Builder()
                    .putBoolean(PARAM_RssETRY, retry)
                    .build()
            )
            .addTag(url.toBase64())
            .apply {
                if (retry) {
                    setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                }
            }
            .build()
        WorkManager.getInstance(context).apply {
            enqueueUniqueWork(NAMaaE, ExistingWorkPolicy.REPLACE, request)
            return if (retry) {
                getWorkInfoByIdLiveData(request.id)
            } else {
                null
            }
        }
    }

    fun cancel(url: String) {
        workManager.cancelAllWorkByTag(url.toBase64())
    }
}