package com.qtiqo.share.data.repo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.qtiqo.share.data.fake.FakeBackend
import com.qtiqo.share.data.fake.FakeFileRecord
import com.qtiqo.share.data.local.dao.UploadDao
import com.qtiqo.share.data.local.entity.UploadEntity
import com.qtiqo.share.data.prefs.SettingsStore
import com.qtiqo.share.data.remote.api.FilesApi
import com.qtiqo.share.data.remote.api.PublicApi
import com.qtiqo.share.domain.model.FilePrivacy
import com.qtiqo.share.domain.model.PublicFileView
import com.qtiqo.share.domain.model.UploadStatus
import com.qtiqo.share.util.newId
import com.qtiqo.share.util.shareUrlForToken
import com.qtiqo.share.worker.UploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AdminStats(
    val users: Int,
    val files: Int,
    val storageBytes: Long,
    val views: Int
)

@Singleton
class UploadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val uploadDao: UploadDao,
    private val workManager: WorkManager,
    private val fakeBackend: FakeBackend,
    private val settingsStore: SettingsStore,
    private val filesApi: FilesApi,
    private val publicApi: PublicApi,
    private val authRepository: AuthRepository
) {
    fun observeUploads(): Flow<List<UploadEntity>> = uploadDao.observeAll()

    fun observeUpload(id: String): Flow<UploadEntity?> = uploadDao.observeById(id)

    fun observeAdminStats(): Flow<AdminStats> = combine(
        uploadDao.observeCount(),
        uploadDao.observeTotalSize(),
        authRepository.sessionFlow
    ) { count, size, _ ->
        AdminStats(
            users = fakeBackend.usersList().size,
            files = count,
            storageBytes = size,
            views = count * 7 + 13
        )
    }

    suspend fun createQueuedUpload(
        uri: Uri,
        fileName: String,
        mimeType: String?,
        sizeBytes: Long,
        privacy: FilePrivacy = FilePrivacy.PUBLIC,
        downloadEnabled: Boolean = true
    ): String {
        val id = newId()
        val now = System.currentTimeMillis()
        uploadDao.upsert(
            UploadEntity(
                id = id,
                localUri = uri.toString(),
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                status = UploadStatus.QUEUED,
                progress = 0,
                shareToken = null,
                shareUrl = null,
                privacy = privacy,
                downloadEnabled = downloadEnabled,
                createdAt = now,
                updatedAt = now,
                uploadedAt = null,
                revoked = false
            )
        )
        enqueueUpload(id)
        return id
    }

    fun enqueueUpload(uploadId: String) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_UPLOAD_ID to uploadId))
            .addTag(uploadTag(uploadId))
            .build()
        workManager.enqueueUniqueWork(uploadWorkName(uploadId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelUpload(uploadId: String) {
        workManager.cancelUniqueWork(uploadWorkName(uploadId))
    }

    suspend fun markCanceled(uploadId: String) {
        val current = uploadDao.getById(uploadId) ?: return
        uploadDao.upsert(
            current.copy(
                status = UploadStatus.CANCELED,
                updatedAt = System.currentTimeMillis(),
                errorMessage = "Canceled"
            )
        )
    }

    fun retryUpload(uploadId: String) = enqueueUpload(uploadId)

    suspend fun deleteUpload(uploadId: String) {
        cancelUpload(uploadId)
        uploadDao.deleteById(uploadId)
    }

    suspend fun updatePrivacy(uploadId: String, privacy: FilePrivacy) {
        val current = uploadDao.getById(uploadId) ?: return
        val updated = current.copy(privacy = privacy, updatedAt = System.currentTimeMillis())
        uploadDao.upsert(updated)
        if (settingsStore.useFakeBackend.value) {
            fakeBackend.getFile(uploadId)?.apply { this.privacy = privacy.name }
        } else {
            runCatching { filesApi.patchFile(uploadId, com.qtiqo.share.data.remote.dto.PatchFileRequest(privacy = privacy.name)) }
        }
    }

    suspend fun updateDownloadEnabled(uploadId: String, enabled: Boolean) {
        val current = uploadDao.getById(uploadId) ?: return
        val updated = current.copy(downloadEnabled = enabled, updatedAt = System.currentTimeMillis())
        uploadDao.upsert(updated)
        if (settingsStore.useFakeBackend.value) {
            fakeBackend.getFile(uploadId)?.apply { this.downloadEnabled = enabled }
        } else {
            runCatching { filesApi.patchFile(uploadId, com.qtiqo.share.data.remote.dto.PatchFileRequest(downloadEnabled = enabled)) }
        }
    }

    suspend fun revokeLink(uploadId: String) {
        val current = uploadDao.getById(uploadId) ?: return
        if (settingsStore.useFakeBackend.value) {
            fakeBackend.revoke(uploadId)
        } else {
            runCatching { filesApi.revoke(uploadId) }
        }
        uploadDao.upsert(
            current.copy(
                revoked = true,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun regenerateLink(uploadId: String) {
        val current = uploadDao.getById(uploadId) ?: return
        val (token, url) = if (settingsStore.useFakeBackend.value) {
            val record = fakeBackend.regenerate(uploadId)
            record.shareToken to shareUrlForToken(record.shareToken)
        } else {
            val response = filesApi.regenerate(uploadId)
            response.shareToken to response.shareUrl
        }
        uploadDao.upsert(
            current.copy(
                shareToken = token,
                shareUrl = url,
                revoked = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun resolvePublicToken(token: String): Result<PublicFileView> = runCatching {
        if (settingsStore.useFakeBackend.value) {
            val local = uploadDao.getByShareToken(token)
            val record = fakeBackend.findByShareToken(token)
            if (local == null || record == null || record.revoked || fakeBackend.isRevokedToken(token)) {
                error("Link is invalid or revoked")
            }
            PublicFileView(
                fileId = local.id,
                name = local.fileName,
                mimeType = local.mimeType,
                sizeBytes = local.sizeBytes,
                localUri = local.localUri,
                shareToken = token,
                allowDownloads = local.downloadEnabled,
                isImage = local.mimeType?.startsWith("image/") == true,
                isRevoked = false
            )
        } else {
            val dto = publicApi.getPublicFile(token)
            PublicFileView(
                fileId = dto.id,
                name = dto.name,
                mimeType = dto.mimeType,
                sizeBytes = dto.sizeBytes,
                localUri = dto.viewUrl ?: dto.downloadUrl,
                shareToken = dto.shareToken,
                allowDownloads = dto.allowDownloads,
                isImage = dto.mimeType?.startsWith("image/") == true,
                isRevoked = dto.revoked
            )
        }
    }

    suspend fun getUploadNow(id: String): UploadEntity? = uploadDao.getById(id)

    suspend fun runUploadWorker(uploadId: String, onProgress: suspend (Int) -> Unit): Result<Unit> = runCatching {
        val entity = uploadDao.getById(uploadId) ?: error("Upload not found")
        val now = System.currentTimeMillis()
        uploadDao.upsert(
            entity.copy(
                status = UploadStatus.UPLOADING,
                progress = entity.progress.coerceAtLeast(1),
                updatedAt = now,
                errorMessage = null
            )
        )
        if (settingsStore.useFakeBackend.value) {
            simulateFakeUpload(entity, onProgress)
        } else {
            performRealUploadPlaceholder(entity, onProgress)
        }
    }

    private suspend fun simulateFakeUpload(
        entity: UploadEntity,
        onProgress: suspend (Int) -> Unit
    ) {
        for (progress in (entity.progress.coerceAtLeast(0) + 5)..100 step 5) {
            delay(180)
            onProgress(progress.coerceAtMost(100))
            val current = uploadDao.getById(entity.id) ?: return
            uploadDao.upsert(
                current.copy(
                    status = UploadStatus.UPLOADING,
                    progress = progress.coerceAtMost(100),
                    updatedAt = System.currentTimeMillis(),
                    errorMessage = null
                )
            )
        }
        val token = com.qtiqo.share.util.generateShareToken()
        val completedAt = System.currentTimeMillis()
        fakeBackend.createOrUpdateFile(
            FakeFileRecord(
                id = entity.id,
                owner = authRepository.sessionFlow.value?.identifier ?: "unknown",
                name = entity.fileName,
                mimeType = entity.mimeType,
                sizeBytes = entity.sizeBytes,
                shareToken = token,
                privacy = entity.privacy.name,
                downloadEnabled = entity.downloadEnabled,
                revoked = false,
                localUri = entity.localUri,
                createdAt = entity.createdAt,
                uploadedAt = completedAt
            )
        )
        val current = uploadDao.getById(entity.id) ?: return
        uploadDao.upsert(
            current.copy(
                status = UploadStatus.DONE,
                progress = 100,
                shareToken = token,
                shareUrl = shareUrlForToken(token),
                uploadedAt = completedAt,
                updatedAt = completedAt,
                revoked = false
            )
        )
    }

    private suspend fun performRealUploadPlaceholder(
        entity: UploadEntity,
        onProgress: suspend (Int) -> Unit
    ) {
        val uri = Uri.parse(entity.localUri)
        contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            var totalRead = 0L
            val totalBytes = entity.sizeBytes.takeIf { it > 0 } ?: 1L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                totalRead += read
                val progress = ((totalRead.toDouble() / totalBytes.toDouble()) * 100).roundToInt().coerceIn(1, 99)
                onProgress(progress)
                val current = uploadDao.getById(entity.id) ?: return
                uploadDao.upsert(current.copy(status = UploadStatus.UPLOADING, progress = progress, updatedAt = System.currentTimeMillis()))
                delay(30)
            }
        } ?: error("Unable to read file")
        throw UnsupportedOperationException("Real backend upload flow is defined but not connected in MVP demo. Enable FakeBackend in Settings.")
    }

    suspend fun markFailed(uploadId: String, message: String?) {
        val current = uploadDao.getById(uploadId) ?: return
        uploadDao.upsert(
            current.copy(
                status = UploadStatus.FAILED,
                updatedAt = System.currentTimeMillis(),
                errorMessage = message ?: "Upload failed"
            )
        )
    }

    suspend fun markProgress(uploadId: String, progress: Int) {
        val current = uploadDao.getById(uploadId) ?: return
        uploadDao.upsert(
            current.copy(
                status = UploadStatus.UPLOADING,
                progress = progress.coerceIn(0, 100),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun fakeUsersList(): List<Pair<String, String>> =
        fakeBackend.usersList().map { it.identifier to it.role.name }

    companion object {
        private const val DEFAULT_BUFFER = 8 * 1024
        fun uploadWorkName(id: String) = "upload_work_$id"
        fun uploadTag(id: String) = "upload_tag_$id"
    }
}
