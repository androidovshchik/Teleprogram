package defpackage.teleprogram.api

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.squareup.duktape.Duktape
import kotlinx.coroutines.coroutineScope

class ApiWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
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

    companion object {

        const val PARAM_RETRY = "retry"

        const val PARAM_JAVASCRIPT = "javascript"

        private const val RESULT_OK = 0
    }
}

class CronManager(context: Context) {

    val workManager = WorkManager.getInstance(context)

    fun launchUnique(retry: Boolean = false) {
        /*val request = OneTimeWorkRequestBuilder<ApiWorker>()
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
        }*/
    }

    fun cancel(context: Context) {
        workManager.cancelAllWork()
    }
}