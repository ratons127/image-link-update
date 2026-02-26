package com.qtiqo.share.data.repo

import com.qtiqo.share.data.fake.FakeBackend
import com.qtiqo.share.data.prefs.SecureSessionStore
import com.qtiqo.share.data.prefs.SettingsStore
import com.qtiqo.share.data.remote.api.AuthApi
import com.qtiqo.share.data.remote.dto.ForgotPasswordRequest
import com.qtiqo.share.data.remote.dto.LoginRequest
import com.qtiqo.share.data.remote.dto.LogoutRequest
import com.qtiqo.share.data.remote.dto.SignUpRequest
import com.qtiqo.share.domain.model.UserRole
import com.qtiqo.share.domain.model.UserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val fakeBackend: FakeBackend,
    private val settingsStore: SettingsStore,
    private val secureSessionStore: SecureSessionStore
) {
    val sessionFlow: StateFlow<UserSession?> = secureSessionStore.sessionFlow
    val useFakeBackend: StateFlow<Boolean> = settingsStore.useFakeBackend

    suspend fun signUp(identifier: String, password: String, phone: String?): Result<Unit> = runCatching {
        if (useFakeBackend.value) {
            val (token, role) = fakeBackend.signUp(identifier.trim(), password, phone?.ifBlank { null })
            secureSessionStore.save(UserSession(token, identifier.trim(), role))
        } else {
            val response = authApi.signUp(SignUpRequest(identifier.trim(), password, phone?.ifBlank { null }))
            secureSessionStore.save(
                UserSession(
                    token = response.token,
                    identifier = response.userId,
                    role = response.role.toUserRole()
                )
            )
        }
    }

    suspend fun login(identifier: String, password: String): Result<Unit> = runCatching {
        if (useFakeBackend.value) {
            val (token, role) = fakeBackend.login(identifier.trim(), password)
            secureSessionStore.save(UserSession(token, identifier.trim(), role))
        } else {
            val response = authApi.login(LoginRequest(identifier.trim(), password))
            secureSessionStore.save(
                UserSession(response.token, response.userId, response.role.toUserRole())
            )
        }
    }

    suspend fun forgot(identifier: String): Result<Unit> = runCatching {
        if (useFakeBackend.value) {
            fakeBackend.forgot(identifier.trim())
        } else {
            authApi.forgot(ForgotPasswordRequest(identifier.trim()))
        }
    }

    suspend fun logout(): Result<Unit> = runCatching {
        if (!useFakeBackend.value) {
            authApi.logout(LogoutRequest(secureSessionStore.sessionFlow.value?.token))
        }
        secureSessionStore.clear()
    }

    fun setFakeBackend(enabled: Boolean) = settingsStore.setUseFakeBackend(enabled)
}

private fun String.toUserRole(): UserRole =
    if (equals("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.USER
