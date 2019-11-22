package defpackage.teleprogram.api

import android.content.Context
import androidx.work.*
import com.squareup.duktape.Duktape
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit

class ApiWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val retry = inputData.getBoolean(PARAM_RETRY, false)
        Duktape.create().use {
            it.set("Android", Android::class.java, ApiEvaluator(applicationContext))
            when {
                it.evaluate("") == 0 -> Result.success()
                retry -> Result.retry()
                else -> Result.failure()
            }
        }
    }

    companion object {

        const val PARAM_RETRY = "retry"
    }
}

class CronManager(context: Context) {

    fun launch(context: Context, retry: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<ApiWorker>()
            .setInputData(
                Data.Builder()
                    .putBoolean(PARAM_RssETRY, retry)
                    .build()
            )
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

    fun cancel(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(NAMaaE)
        }
    }
}