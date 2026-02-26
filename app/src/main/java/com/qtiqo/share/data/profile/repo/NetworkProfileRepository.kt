package com.qtiqo.share.data.profile.repo

import com.qtiqo.share.data.profile.api.ProfileApi
import com.qtiqo.share.data.profile.dto.ChangePasswordRequestDto
import com.qtiqo.share.data.profile.dto.LogoutRequestDto
import com.qtiqo.share.feature.profile.domain.models.ProfileSummary
import com.qtiqo.share.feature.profile.integration.SessionManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class NetworkProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val sessionManager: SessionManager
) : ProfileRepository {
    private suspend fun bearer(): String {
        val session = sessionManager.sessionFlow.first() ?: error("Not authorized")
        if (session.token.isBlank()) error("Not authorized")
        return "Bearer ${session.token}"
    }

    override suspend fun getSummary(): ProfileSummary {
        val dto = api.getMeSummary(bearer())
        return ProfileSummary(
            filesCount = dto.filesCount,
            storageUsedBytes = dto.storageUsedBytes,
            storageLimitBytes = dto.storageLimitBytes,
            uploadLimitBytes = dto.uploadLimitBytes
        )
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        api.changePassword(
            bearer = bearer(),
            request = ChangePasswordRequestDto(currentPassword = currentPassword, newPassword = newPassword)
        )
    }

    override suspend fun logout() {
        val session = sessionManager.sessionFlow.first()
        val bearer = bearer()
        api.logout(bearer = bearer, request = LogoutRequestDto(token = session?.token))
        sessionManager.clearSession()
    }
}

