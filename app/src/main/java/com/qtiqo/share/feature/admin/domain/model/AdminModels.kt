package com.qtiqo.share.feature.admin.domain.model

enum class AdminRole { ADMIN, USER }
enum class AdminPlan { FREE }
enum class AdminPrivacy { PUBLIC, UNLISTED, PRIVATE }
enum class AdminTargetType { USER, FILE }
enum class AdminLogEventType { LOGIN, UPLOAD, PUBLIC_VIEW, DOWNLOAD, REVOKE_LINK, DELETE_FILE, USER_SUSPENDED }

data class AdminStats(
    val totalUsers: Int,
    val totalFiles: Int,
    val totalStorageUsedBytes: Long,
    val totalViews: Long
)

data class AdminUser(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val role: AdminRole,
    val plan: AdminPlan = AdminPlan.FREE,
    val isSuspended: Boolean,
    val storageUsedBytes: Long,
    val storageLimitBytes: Long,
    val maxUploadSizeBytes: Long,
    val fileCount: Int,
    val createdAt: Long
)

data class AdminFile(
    val id: String,
    val ownerId: String,
    val ownerEmail: String,
    val filename: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val privacy: AdminPrivacy,
    val downloadEnabled: Boolean,
    val views: Long,
    val createdAt: Long,
    val shareToken: String? = null,
    val shareUrl: String? = null,
    val previewUrl: String? = null,
    val isRevoked: Boolean = false
)

data class AdminLog(
    val id: String,
    val eventType: AdminLogEventType,
    val actorUserId: String,
    val actorEmail: String,
    val targetType: AdminTargetType,
    val targetId: String,
    val message: String,
    val ip: String?,
    val device: String?,
    val createdAt: Long
)

data class AdminSettings(
    val defaultStorageLimitBytes: Long,
    val maxUploadSizeBytes: Long,
    val downloadsEnabledByDefault: Boolean,
    val publicPagesEnabled: Boolean,
    val rateLimitPerMinute: Int,
    val captchaEnabled: Boolean
)

data class PagedResult<T>(
    val items: List<T>,
    val nextPage: Int?
)

data class CreateAdminUserInput(
    val name: String,
    val email: String,
    val phone: String?,
    val password: String,
    val role: AdminRole,
    val storageLimitBytes: Long,
    val maxUploadSizeBytes: Long
)

data class UpdateAdminUserInput(
    val role: AdminRole? = null,
    val isSuspended: Boolean? = null,
    val storageLimitBytes: Long? = null,
    val maxUploadSizeBytes: Long? = null
)

data class UpdateAdminFileInput(
    val privacy: AdminPrivacy? = null,
    val downloadEnabled: Boolean? = null
)

data class UserQuery(
    val query: String = "",
    val page: Int = 1,
    val pageSize: Int = 20
)

enum class ContentSort { NEWEST, OLDEST, LARGEST, MOST_VIEWED }

data class FileQuery(
    val query: String = "",
    val owner: String? = null,
    val privacy: AdminPrivacy? = null,
    val sort: ContentSort = ContentSort.NEWEST,
    val page: Int = 1,
    val pageSize: Int = 20
)

data class LogQuery(
    val eventType: AdminLogEventType? = null,
    val actor: String = "",
    val target: String = "",
    val page: Int = 1,
    val pageSize: Int = 20
)
