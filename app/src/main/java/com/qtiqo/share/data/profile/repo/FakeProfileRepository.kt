package com.qtiqo.share.data.profile.repo

import com.qtiqo.share.feature.profile.domain.models.ProfileSummary
import com.qtiqo.share.feature.profile.integration.SessionManager
import javax.inject.Inject
import kotlinx.coroutines.delay

class FakeProfileRepository @Inject constructor(
    private val sessionManager: SessionManager
) : ProfileRepository {
    override suspend fun getSummary(): ProfileSummary {
        delay(200)
        return ProfileSummary(
            filesCount = 3,
            storageUsedBytes = (1.2 * 1024 * 1024).toLong(),
            storageLimitBytes = 100L * 1024 * 1024,
            uploadLimitBytes = 10L * 1024 * 1024
        )
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        delay(350)
        if (currentPassword != "demo1234" && currentPassword != "demo123") {
            error("Current password is incorrect")
        }
        if (newPassword.length < 8) error("New password must be at least 8 characters")
    }

    override suspend fun logout() {
        delay(150)
        sessionManager.clearSession()
    }
}

