package com.qtiqo.share.feature.profile.integration

import com.qtiqo.share.feature.profile.domain.models.ProfileSession
import kotlinx.coroutines.flow.Flow

interface SessionManager {
    val sessionFlow: Flow<ProfileSession?>
    suspend fun clearSession()
}

