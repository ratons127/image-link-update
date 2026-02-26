package com.qtiqo.share.feature.admin.domain.usecase

import com.qtiqo.share.feature.admin.integration.SessionManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class EnsureAdminAccessUseCase @Inject constructor(
    private val sessionManager: SessionManager
) {
    suspend fun requireAdminToken(): String {
        val session = sessionManager.sessionFlow.first() ?: error("Not authorized")
        if (!session.role.equals("ADMIN", ignoreCase = true)) error("Not authorized")
        if (session.token.isBlank()) error("Not authorized")
        return session.token
    }
}
