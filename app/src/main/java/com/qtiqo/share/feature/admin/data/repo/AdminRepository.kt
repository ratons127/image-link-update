package com.qtiqo.share.feature.admin.data.repo

import com.qtiqo.share.feature.admin.domain.model.AdminFile
import com.qtiqo.share.feature.admin.domain.model.AdminSettings
import com.qtiqo.share.feature.admin.domain.model.AdminStats
import com.qtiqo.share.feature.admin.domain.model.AdminLog
import com.qtiqo.share.feature.admin.domain.model.AdminUser
import com.qtiqo.share.feature.admin.domain.model.CreateAdminUserInput
import com.qtiqo.share.feature.admin.domain.model.FileQuery
import com.qtiqo.share.feature.admin.domain.model.LogQuery
import com.qtiqo.share.feature.admin.domain.model.PagedResult
import com.qtiqo.share.feature.admin.domain.model.UpdateAdminFileInput
import com.qtiqo.share.feature.admin.domain.model.UpdateAdminUserInput
import com.qtiqo.share.feature.admin.domain.model.UserQuery

interface AdminRepository {
    suspend fun getStats(): AdminStats
    suspend fun getUsers(query: UserQuery): PagedResult<AdminUser>
    suspend fun createUser(input: CreateAdminUserInput): AdminUser
    suspend fun updateUser(userId: String, input: UpdateAdminUserInput): AdminUser
    suspend fun deleteUser(userId: String)

    suspend fun getFiles(query: FileQuery): PagedResult<AdminFile>
    suspend fun updateFile(fileId: String, input: UpdateAdminFileInput): AdminFile
    suspend fun deleteFile(fileId: String)
    suspend fun revokeFileLink(fileId: String): AdminFile

    suspend fun getLogs(query: LogQuery): PagedResult<AdminLog>

    suspend fun getSettings(): AdminSettings
    suspend fun updateSettings(settings: AdminSettings): AdminSettings
}
