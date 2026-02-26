package com.qtiqo.share.feature.admin.data.repo

import com.qtiqo.share.feature.admin.data.fake.FakeAdminBackend
import com.qtiqo.share.feature.admin.domain.model.*
import com.qtiqo.share.feature.admin.domain.usecase.EnsureAdminAccessUseCase
import javax.inject.Inject

class FakeAdminRepository @Inject constructor(
    private val backend: FakeAdminBackend,
    private val ensureAdminAccessUseCase: EnsureAdminAccessUseCase
) : AdminRepository {
    private suspend fun requireAdmin() {
        ensureAdminAccessUseCase.requireAdminToken()
    }

    override suspend fun getStats(): AdminStats { requireAdmin(); return backend.stats() }
    override suspend fun getUsers(query: UserQuery): PagedResult<AdminUser> { requireAdmin(); return backend.getUsers(query) }
    override suspend fun createUser(input: CreateAdminUserInput): AdminUser { requireAdmin(); return backend.createUser(input) }
    override suspend fun updateUser(userId: String, input: UpdateAdminUserInput): AdminUser { requireAdmin(); return backend.updateUser(userId, input) }
    override suspend fun deleteUser(userId: String) { requireAdmin(); backend.deleteUser(userId) }
    override suspend fun getFiles(query: FileQuery): PagedResult<AdminFile> { requireAdmin(); return backend.getFiles(query) }
    override suspend fun updateFile(fileId: String, input: UpdateAdminFileInput): AdminFile { requireAdmin(); return backend.updateFile(fileId, input) }
    override suspend fun deleteFile(fileId: String) { requireAdmin(); backend.deleteFile(fileId) }
    override suspend fun revokeFileLink(fileId: String): AdminFile { requireAdmin(); return backend.revokeFileLink(fileId) }
    override suspend fun getLogs(query: LogQuery): PagedResult<AdminLog> { requireAdmin(); return backend.getLogs(query) }
    override suspend fun getSettings(): AdminSettings { requireAdmin(); return backend.getSettings() }
    override suspend fun updateSettings(settings: AdminSettings): AdminSettings { requireAdmin(); return backend.updateSettings(settings) }
}
