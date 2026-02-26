package com.qtiqo.share.feature.admin.data.repo

import com.qtiqo.share.feature.admin.data.api.AdminApi
import com.qtiqo.share.feature.admin.data.dto.AdminDtos
import com.qtiqo.share.feature.admin.domain.model.*
import com.qtiqo.share.feature.admin.domain.usecase.EnsureAdminAccessUseCase
import javax.inject.Inject

class NetworkAdminRepository @Inject constructor(
    private val api: AdminApi,
    private val ensureAdminAccessUseCase: EnsureAdminAccessUseCase
) : AdminRepository {

    private suspend fun requireAdmin() {
        ensureAdminAccessUseCase.requireAdminToken()
    }
    private suspend fun bearer(): String = "Bearer ${ensureAdminAccessUseCase.requireAdminToken()}"

    override suspend fun getStats(): AdminStats {
        return api.getStats(bearer()).toModel()
    }

    override suspend fun getUsers(query: UserQuery): PagedResult<AdminUser> {
        val response = api.getUsers(bearer(), query.query.ifBlank { null }, query.page, query.pageSize)
        return PagedResult(response.items.map { it.toModel() }, response.nextPage)
    }

    override suspend fun createUser(input: CreateAdminUserInput): AdminUser {
        return api.createUser(
            bearer(),
            AdminDtos.CreateUserRequestDto(
                name = input.name,
                email = input.email,
                phone = input.phone,
                password = input.password,
                role = input.role.name,
                storageLimitBytes = input.storageLimitBytes,
                maxUploadSizeBytes = input.maxUploadSizeBytes
            )
        ).toModel()
    }

    override suspend fun updateUser(userId: String, input: UpdateAdminUserInput): AdminUser {
        return api.updateUser(
            bearer(),
            userId,
            AdminDtos.UpdateUserRequestDto(
                role = input.role?.name,
                isSuspended = input.isSuspended,
                storageLimitBytes = input.storageLimitBytes,
                maxUploadSizeBytes = input.maxUploadSizeBytes
            )
        ).toModel()
    }

    override suspend fun deleteUser(userId: String) {
        api.deleteUser(bearer(), userId)
    }

    override suspend fun getFiles(query: FileQuery): PagedResult<AdminFile> {
        val res = api.getFiles(
            bearer = bearer(),
            query = query.query.ifBlank { null },
            owner = query.owner,
            privacy = query.privacy?.name,
            sort = query.sort.name,
            page = query.page,
            pageSize = query.pageSize
        )
        return PagedResult(res.items.map { it.toModel() }, res.nextPage)
    }

    override suspend fun updateFile(fileId: String, input: UpdateAdminFileInput): AdminFile {
        return api.updateFile(
            bearer(),
            fileId,
            AdminDtos.UpdateFileRequestDto(
                privacy = input.privacy?.name,
                downloadEnabled = input.downloadEnabled
            )
        ).toModel()
    }

    override suspend fun deleteFile(fileId: String) {
        api.deleteFile(bearer(), fileId)
    }

    override suspend fun revokeFileLink(fileId: String): AdminFile {
        return api.updateFile(bearer(), fileId, AdminDtos.UpdateFileRequestDto()).toModel().copy(isRevoked = true)
    }

    override suspend fun getLogs(query: LogQuery): PagedResult<AdminLog> {
        val res = api.getLogs(
            bearer = bearer(),
            eventType = query.eventType?.name,
            actor = query.actor.ifBlank { null },
            target = query.target.ifBlank { null },
            page = query.page,
            pageSize = query.pageSize
        )
        return PagedResult(res.items.map { it.toModel() }, res.nextPage)
    }

    override suspend fun getSettings(): AdminSettings {
        return api.getSettings(bearer()).toModel()
    }

    override suspend fun updateSettings(settings: AdminSettings): AdminSettings {
        return api.updateSettings(bearer(), settings.toDto()).toModel()
    }
}

private fun AdminDtos.AdminStatsDto.toModel() = AdminStats(totalUsers, totalFiles, totalStorageUsedBytes, totalViews)
private fun AdminDtos.AdminUserDto.toModel() = AdminUser(
    id, name, email, phone,
    role = if (role.equals("ADMIN", true)) AdminRole.ADMIN else AdminRole.USER,
    plan = AdminPlan.FREE,
    isSuspended = isSuspended,
    storageUsedBytes = storageUsedBytes,
    storageLimitBytes = storageLimitBytes,
    maxUploadSizeBytes = maxUploadSizeBytes,
    fileCount = fileCount,
    createdAt = createdAt
)
private fun AdminDtos.AdminFileDto.toModel() = AdminFile(
    id, ownerId, ownerEmail, filename, mimeType, sizeBytes,
    privacy = AdminPrivacy.valueOf(privacy),
    downloadEnabled = downloadEnabled,
    views = views,
    createdAt = createdAt,
    shareToken = shareToken,
    shareUrl = shareUrl,
    previewUrl = previewUrl,
    isRevoked = isRevoked
)
private fun AdminDtos.AdminLogDto.toModel() = AdminLog(
    id = id,
    eventType = AdminLogEventType.valueOf(eventType),
    actorUserId = actorUserId,
    actorEmail = actorEmail,
    targetType = AdminTargetType.valueOf(targetType),
    targetId = targetId,
    message = message,
    ip = ip,
    device = device,
    createdAt = createdAt
)
private fun AdminDtos.AdminSettingsDto.toModel() = AdminSettings(
    defaultStorageLimitBytes,
    maxUploadSizeBytes,
    downloadsEnabledByDefault,
    publicPagesEnabled,
    rateLimitPerMinute,
    captchaEnabled
)
private fun AdminSettings.toDto() = AdminDtos.AdminSettingsDto(
    defaultStorageLimitBytes,
    maxUploadSizeBytes,
    downloadsEnabledByDefault,
    publicPagesEnabled,
    rateLimitPerMinute,
    captchaEnabled
)
