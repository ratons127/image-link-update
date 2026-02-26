package com.qtiqo.share.feature.admin.data.fake

import com.qtiqo.share.feature.admin.domain.model.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

@Singleton
class FakeAdminBackend @Inject constructor() {
    private val users = mutableListOf<AdminUser>()
    private val files = mutableListOf<AdminFile>()
    private val logs = mutableListOf<AdminLog>()
    private var settings = AdminSettings(
        defaultStorageLimitBytes = 2L * 1024 * 1024 * 1024,
        maxUploadSizeBytes = 250L * 1024 * 1024,
        downloadsEnabledByDefault = true,
        publicPagesEnabled = true,
        rateLimitPerMinute = 120,
        captchaEnabled = false
    )

    init {
        seed()
    }

    @Synchronized
    fun stats(): AdminStats = AdminStats(
        totalUsers = users.size,
        totalFiles = files.size,
        totalStorageUsedBytes = users.sumOf { it.storageUsedBytes },
        totalViews = files.sumOf { it.views }
    )

    @Synchronized
    fun getUsers(query: UserQuery): PagedResult<AdminUser> {
        val filtered = users
            .filter {
                query.query.isBlank() ||
                    it.name.contains(query.query, true) ||
                    it.email.contains(query.query, true)
            }
            .sortedByDescending { it.createdAt }
        return paginate(filtered, query.page, query.pageSize)
    }

    @Synchronized
    fun createUser(input: CreateAdminUserInput): AdminUser {
        val user = AdminUser(
            id = newId(),
            name = input.name,
            email = input.email,
            phone = input.phone,
            role = input.role,
            plan = AdminPlan.FREE,
            isSuspended = false,
            storageUsedBytes = 0L,
            storageLimitBytes = input.storageLimitBytes,
            maxUploadSizeBytes = input.maxUploadSizeBytes,
            fileCount = 0,
            createdAt = System.currentTimeMillis()
        )
        users += user
        logs += newLog(AdminLogEventType.LOGIN, "system", "admin@qtiqo.com", AdminTargetType.USER, user.id, "Admin created user ${user.email}")
        return user
    }

    @Synchronized
    fun updateUser(userId: String, input: UpdateAdminUserInput): AdminUser {
        val idx = users.indexOfFirst { it.id == userId }
        if (idx < 0) error("User not found")
        val current = users[idx]
        val updated = current.copy(
            role = input.role ?: current.role,
            isSuspended = input.isSuspended ?: current.isSuspended,
            storageLimitBytes = input.storageLimitBytes ?: current.storageLimitBytes,
            maxUploadSizeBytes = input.maxUploadSizeBytes ?: current.maxUploadSizeBytes
        )
        users[idx] = updated
        if (input.isSuspended == true || input.isSuspended == false) {
            logs += newLog(
                AdminLogEventType.USER_SUSPENDED,
                "system",
                "admin@qtiqo.com",
                AdminTargetType.USER,
                updated.id,
                if (updated.isSuspended) "Suspended user ${updated.email}" else "Activated user ${updated.email}"
            )
        }
        return updated
    }

    @Synchronized
    fun deleteUser(userId: String) {
        val user = users.firstOrNull { it.id == userId } ?: error("User not found")
        users.removeAll { it.id == userId }
        val owned = files.filter { it.ownerId == userId }.map { it.id }
        files.removeAll { it.ownerId == userId }
        logs += newLog(AdminLogEventType.DELETE_FILE, "system", "admin@qtiqo.com", AdminTargetType.USER, userId, "Deleted user ${user.email} and ${owned.size} files")
    }

    @Synchronized
    fun getFiles(query: FileQuery): PagedResult<AdminFile> {
        var filtered = files.asSequence()
        if (query.query.isNotBlank()) filtered = filtered.filter { it.filename.contains(query.query, true) }
        if (!query.owner.isNullOrBlank()) filtered = filtered.filter { it.ownerEmail.contains(query.owner, true) || it.ownerId == query.owner }
        if (query.privacy != null) filtered = filtered.filter { it.privacy == query.privacy }
        val sorted = when (query.sort) {
            ContentSort.NEWEST -> filtered.sortedByDescending { it.createdAt }
            ContentSort.OLDEST -> filtered.sortedBy { it.createdAt }
            ContentSort.LARGEST -> filtered.sortedByDescending { it.sizeBytes }
            ContentSort.MOST_VIEWED -> filtered.sortedByDescending { it.views }
        }.toList()
        return paginate(sorted, query.page, query.pageSize)
    }

    @Synchronized
    fun updateFile(fileId: String, input: UpdateAdminFileInput): AdminFile {
        val idx = files.indexOfFirst { it.id == fileId }
        if (idx < 0) error("File not found")
        val current = files[idx]
        val updated = current.copy(
            privacy = input.privacy ?: current.privacy,
            downloadEnabled = input.downloadEnabled ?: current.downloadEnabled
        )
        files[idx] = updated
        return updated
    }

    @Synchronized
    fun deleteFile(fileId: String) {
        val file = files.firstOrNull { it.id == fileId } ?: error("File not found")
        files.removeAll { it.id == fileId }
        val uIdx = users.indexOfFirst { it.id == file.ownerId }
        if (uIdx >= 0) {
            val u = users[uIdx]
            users[uIdx] = u.copy(
                storageUsedBytes = (u.storageUsedBytes - file.sizeBytes).coerceAtLeast(0L),
                fileCount = (u.fileCount - 1).coerceAtLeast(0)
            )
        }
        logs += newLog(AdminLogEventType.DELETE_FILE, "system", "admin@qtiqo.com", AdminTargetType.FILE, fileId, "Deleted file ${file.filename}")
    }

    @Synchronized
    fun revokeFileLink(fileId: String): AdminFile {
        val idx = files.indexOfFirst { it.id == fileId }
        if (idx < 0) error("File not found")
        val current = files[idx]
        val updated = current.copy(
            shareToken = current.shareToken?.let { "revoked_$it" },
            shareUrl = null,
            isRevoked = true
        )
        files[idx] = updated
        logs += newLog(AdminLogEventType.REVOKE_LINK, "system", "admin@qtiqo.com", AdminTargetType.FILE, fileId, "Revoked public link for ${current.filename}")
        return updated
    }

    @Synchronized
    fun getLogs(query: LogQuery): PagedResult<AdminLog> {
        val filtered = logs
            .asSequence()
            .filter { query.eventType == null || it.eventType == query.eventType }
            .filter { query.actor.isBlank() || it.actorEmail.contains(query.actor, true) }
            .filter { query.target.isBlank() || it.targetId.contains(query.target, true) }
            .sortedByDescending { it.createdAt }
            .toList()
        return paginate(filtered, query.page, query.pageSize)
    }

    @Synchronized
    fun getSettings(): AdminSettings = settings

    @Synchronized
    fun updateSettings(newSettings: AdminSettings): AdminSettings {
        settings = newSettings
        return settings
    }

    private fun seed() {
        if (users.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val seededUsers = (0 until 10).map { idx ->
            val isAdmin = idx == 0
            val limit = if (isAdmin) 10L * 1024 * 1024 * 1024 else (1L + idx) * 1024 * 1024 * 1024
            val used = (limit * (20 + idx * 5) / 100.0).toLong()
            AdminUser(
                id = "u_${idx + 1}",
                name = if (isAdmin) "Admin User" else "User ${idx}",
                email = if (isAdmin) "admin@qtiqo.com" else "user${idx}@qtiqo.com",
                phone = null,
                role = if (isAdmin) AdminRole.ADMIN else AdminRole.USER,
                isSuspended = idx == 7,
                storageUsedBytes = used,
                storageLimitBytes = limit,
                maxUploadSizeBytes = 250L * 1024 * 1024,
                fileCount = 0,
                createdAt = now - idx * 86_400_000L
            )
        }
        users += seededUsers

        repeat(25) { i ->
            val owner = users[(i % 9) + 1]
            val mime = listOf("image/jpeg", "image/png", "video/mp4", "application/pdf", "application/zip")[i % 5]
            val token = UUID.randomUUID().toString().replace("-", "").take(12)
            val file = AdminFile(
                id = "f_${i + 1}",
                ownerId = owner.id,
                ownerEmail = owner.email,
                filename = when {
                    mime.startsWith("image/") -> "photo_${i + 1}.jpg"
                    mime.startsWith("video/") -> "clip_${i + 1}.mp4"
                    mime == "application/pdf" -> "report_${i + 1}.pdf"
                    else -> "archive_${i + 1}.zip"
                },
                mimeType = mime,
                sizeBytes = (2_000_000L + i * 850_000L),
                privacy = AdminPrivacy.entries[i % AdminPrivacy.entries.size],
                downloadEnabled = i % 4 != 0,
                views = (i * 13L) + 7L,
                createdAt = now - i * 2_700_000L,
                shareToken = token,
                shareUrl = "https://imagelink.qtiqo.com/s/$token",
                previewUrl = if (mime.startsWith("image/") || mime.startsWith("video/")) "https://picsum.photos/seed/$i/900/600" else null,
                isRevoked = false
            )
            files += file
        }

        // Recompute user usage from files
        users.replaceAll { u ->
            val owned = files.filter { it.ownerId == u.id }
            u.copy(fileCount = owned.size, storageUsedBytes = min(u.storageLimitBytes, owned.sumOf { it.sizeBytes }))
        }

        val events = AdminLogEventType.entries
        repeat(100) { i ->
            val actor = users[i % users.size]
            val file = files.getOrNull(i % files.size)
            val event = events[i % events.size]
            logs += AdminLog(
                id = "log_${i + 1}",
                eventType = event,
                actorUserId = actor.id,
                actorEmail = actor.email,
                targetType = if (i % 2 == 0) AdminTargetType.FILE else AdminTargetType.USER,
                targetId = file?.id ?: users[(i + 1) % users.size].id,
                message = "${event.name} event recorded for ${actor.email}",
                ip = "192.168.1.${(i % 220) + 10}",
                device = listOf("Android", "Web", "iPhone", "Desktop")[i % 4],
                createdAt = now - i * 45_000L
            )
        }
    }

    private fun newId(): String = UUID.randomUUID().toString()

    private fun newLog(
        eventType: AdminLogEventType,
        actorUserId: String,
        actorEmail: String,
        targetType: AdminTargetType,
        targetId: String,
        message: String
    ) = AdminLog(
        id = newId(),
        eventType = eventType,
        actorUserId = actorUserId,
        actorEmail = actorEmail,
        targetType = targetType,
        targetId = targetId,
        message = message,
        ip = "127.0.0.1",
        device = "AdminPanel",
        createdAt = System.currentTimeMillis()
    )

    private fun <T> paginate(items: List<T>, page: Int, pageSize: Int): PagedResult<T> {
        val start = (page - 1).coerceAtLeast(0) * pageSize
        if (start >= items.size) return PagedResult(emptyList(), null)
        val end = min(start + pageSize, items.size)
        val next = if (end < items.size) page + 1 else null
        return PagedResult(items.subList(start, end), next)
    }
}
