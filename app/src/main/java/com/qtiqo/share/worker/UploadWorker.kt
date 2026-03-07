package com.qtiqo.share.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.qtiqo.share.R
import com.qtiqo.share.data.repo.UploadRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val uploadRepository: UploadRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            UploadWorkerEntryPoint::class.java
        ).uploadRepository()
    }

    override suspend fun doWork(): Result {
        val uploadId = inputData.getString(KEY_UPLOAD_ID) ?: return Result.failure()
        Log.d(TAG, "doWork started for uploadId=$uploadId")
        val result = runCatching {
            setForeground(createForegroundInfo(uploadId, 0))
            uploadRepository.runUploadWorker(uploadId) { progress ->
                setProgressAsync(androidx.work.workDataOf(KEY_PROGRESS to progress))
                setForeground(createForegroundInfo(uploadId, progress))
                uploadRepository.markProgress(uploadId, progress)
            }.getOrThrow()
        }

        return result.fold(
            onSuccess = {
                Log.d(TAG, "doWork success for uploadId=$uploadId")
                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "doWork failure for uploadId=$uploadId: ${error.message}", error)
                if (isStopped) {
                    uploadRepository.markCanceled(uploadId)
                    Result.failure()
                } else {
                    uploadRepository.markFailed(uploadId, error.message)
                    Result.failure()
                }
            }
        )
    }

    private fun createForegroundInfo(uploadId: String, progress: Int): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText("Uploading file... $progress%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                uploadId.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(uploadId.hashCode(), notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Uploads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Foreground notifications for file uploads"
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
            )
        }
    }

    companion object {
        private const val TAG = "UploadWorker"
        const val KEY_UPLOAD_ID = "upload_id"
        const val KEY_PROGRESS = "progress"
        private const val CHANNEL_ID = "uploads"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UploadWorkerEntryPoint {
    fun uploadRepository(): UploadRepository
}
