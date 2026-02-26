package com.qtiqo.share.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.qtiqo.share.R
import com.qtiqo.share.data.repo.UploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val uploadRepository: UploadRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uploadId = inputData.getString(KEY_UPLOAD_ID) ?: return Result.failure()
        setForeground(createForegroundInfo(uploadId, 0))

        val result = uploadRepository.runUploadWorker(uploadId) { progress ->
            setProgressAsync(androidx.work.workDataOf(KEY_PROGRESS to progress))
            setForeground(createForegroundInfo(uploadId, progress))
        }

        return result.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
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
        return ForegroundInfo(uploadId.hashCode(), notification)
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
        const val KEY_UPLOAD_ID = "upload_id"
        const val KEY_PROGRESS = "progress"
        private const val CHANNEL_ID = "uploads"
    }
}
