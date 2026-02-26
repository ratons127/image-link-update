package com.qtiqo.share.data.remote.dto

data class SignUpRequest(
    val identifier: String,
    val password: String,
    val phone: String? = null
)

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class ForgotPasswordRequest(
    val identifier: String
)

data class LogoutRequest(
    val token: String? = null
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val role: String
)
