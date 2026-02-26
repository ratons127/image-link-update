package com.qtiqo.share.feature.admin.data.dto

object AdminDtos {
    data class AdminStatsDto(
        val totalUsers: Int,
        val totalFiles: Int,
        val totalStorageUsedBytes: Long,
        val totalViews: Long
    )

    data class AdminUserDto(
        val id: String,
        val name: String,
        val email: String,
        val phone: String?,
        val role: String,
        val plan: String,
        val isSuspended: Boolean,
        val storageUsedBytes: Long,
        val storageLimitBytes: Long,
        val maxUploadSizeBytes: Long,
        val fileCount: Int,
        val createdAt: Long
    )

    data class AdminFileDto(
        val id: String,
        val ownerId: String,
        val ownerEmail: String,
        val filename: String,
        val mimeType: String?,
        val sizeBytes: Long,
        val privacy: String,
        val downloadEnabled: Boolean,
        val views: Long,
        val createdAt: Long,
        val shareToken: String? = null,
        val shareUrl: String? = null,
        val previewUrl: String? = null,
        val isRevoked: Boolean = false
    )

    data class AdminLogDto(
        val id: String,
        val eventType: String,
        val actorUserId: String,
        val actorEmail: String,
        val targetType: String,
        val targetId: String,
        val message: String,
        val ip: String?,
        val device: String?,
        val createdAt: Long
    )

    data class AdminSettingsDto(
        val defaultStorageLimitBytes: Long,
        val maxUploadSizeBytes: Long,
        val downloadsEnabledByDefault: Boolean,
        val publicPagesEnabled: Boolean,
        val rateLimitPerMinute: Int,
        val captchaEnabled: Boolean
    )

    data class PagedUsersResponseDto(val items: List<AdminUserDto>, val nextPage: Int?)
    data class PagedFilesResponseDto(val items: List<AdminFileDto>, val nextPage: Int?)
    data class PagedLogsResponseDto(val items: List<AdminLogDto>, val nextPage: Int?)

    data class CreateUserRequestDto(
        val name: String,
        val email: String,
        val phone: String?,
        val password: String,
        val role: String,
        val storageLimitBytes: Long,
        val maxUploadSizeBytes: Long
    )

    data class UpdateUserRequestDto(
        val role: String? = null,
        val isSuspended: Boolean? = null,
        val storageLimitBytes: Long? = null,
        val maxUploadSizeBytes: Long? = null
    )

    data class UpdateFileRequestDto(
        val privacy: String? = null,
        val downloadEnabled: Boolean? = null
    )
}
