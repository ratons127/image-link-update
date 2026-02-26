package com.qtiqo.share.domain.model

data class UserSession(
    val token: String,
    val identifier: String,
    val role: UserRole
)
