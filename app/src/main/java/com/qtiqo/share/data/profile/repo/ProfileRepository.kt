package com.qtiqo.share.data.profile.repo

import com.qtiqo.share.feature.profile.domain.models.ProfileSummary

interface ProfileRepository {
    suspend fun getSummary(): ProfileSummary
    suspend fun changePassword(currentPassword: String, newPassword: String)
    suspend fun logout()
}

