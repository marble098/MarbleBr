package com.marble098.marblebr.engine

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Periodically requests a local checkpoint from an already-open Gecko account. */
@HiltWorker
class SmartKeepAliveWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val containerDao: ContainerDao,
    private val sessionManager: ContainerSessionManager,
    private val preferencesRepository: BrowserPreferencesRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!preferencesRepository.preferencesFlow.first().smartKeepAliveEnabled) return Result.success()
        for (container in containerDao.getKeepAliveEligibleContainers()) {
            sessionManager.captureScheduledCheckpoint(container.toDomain())
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "marblebr_smart_keep_alive_periodic_work"
    }
}

@Singleton
class KeepAliveScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedulePeriodicKeepAlive(intervalMinutes: Long = 30L) {
        val safeInterval = intervalMinutes.coerceAtLeast(15L)
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<SmartKeepAliveWorker>(
            safeInterval,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SmartKeepAliveWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancelKeepAlive() {
        WorkManager.getInstance(context).cancelUniqueWork(SmartKeepAliveWorker.WORK_NAME)
    }
}
