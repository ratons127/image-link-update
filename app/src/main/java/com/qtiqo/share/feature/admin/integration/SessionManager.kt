package com.qtiqo.share.feature.admin.integration

import kotlinx.coroutines.flow.Flow

data class Session(
    val userId: String,
    val email: String,
    val role: String,
    val token: String
)

interface SessionManager {
    val sessionFlow: Flow<Session?>
}
