package com.qtiqo.share.feature.admin.data.repo

import com.qtiqo.share.feature.admin.domain.model.*
import com.qtiqo.share.feature.admin.integration.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SwitchingAdminRepository @Inject constructor(
    private val fakeRepo: FakeAdminRepository,
    private val networkRepo: NetworkAdminRepository,
    private val settingsStore: SettingsStore
) : AdminRepository {
    private suspend fun delegate(): AdminRepository =
        if (settingsStore.fakeBackendEnabled.first()) fakeRepo else networkRepo

    override suspend fun getStats(): AdminStats = delegate().getStats()
    override suspend fun getUsers(query: UserQuery): PagedResult<AdminUser> = delegate().getUsers(query)
    override suspend fun createUser(input: CreateAdminUserInput): AdminUser = delegate().createUser(input)
    override suspend fun updateUser(userId: String, input: UpdateAdminUserInput): AdminUser = delegate().updateUser(userId, input)
    override suspend fun deleteUser(userId: String) = delegate().deleteUser(userId)
    override suspend fun getFiles(query: FileQuery): PagedResult<AdminFile> = delegate().getFiles(query)
    override suspend fun updateFile(fileId: String, input: UpdateAdminFileInput): AdminFile = delegate().updateFile(fileId, input)
    override suspend fun deleteFile(fileId: String) = delegate().deleteFile(fileId)
    override suspend fun revokeFileLink(fileId: String): AdminFile = delegate().revokeFileLink(fileId)
    override suspend fun getLogs(query: LogQuery): PagedResult<AdminLog> = delegate().getLogs(query)
    override suspend fun getSettings(): AdminSettings = delegate().getSettings()
    override suspend fun updateSettings(settings: AdminSettings): AdminSettings = delegate().updateSettings(settings)
}
