package com.qtiqo.share.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.qtiqo.share.domain.model.UserRole
import com.qtiqo.share.domain.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _sessionFlow = MutableStateFlow(load())
    val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    fun save(session: UserSession) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_IDENTIFIER, session.identifier)
            .putString(KEY_ROLE, session.role.name)
            .apply()
        _sessionFlow.value = session
    }

    fun clear() {
        prefs.edit().clear().apply()
        _sessionFlow.value = null
    }

    private fun load(): UserSession? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val identifier = prefs.getString(KEY_IDENTIFIER, null) ?: return null
        val roleName = prefs.getString(KEY_ROLE, UserRole.USER.name) ?: UserRole.USER.name
        return UserSession(token = token, identifier = identifier, role = UserRole.valueOf(roleName))
    }

    private companion object {
        const val KEY_TOKEN = "jwt_token"
        const val KEY_IDENTIFIER = "identifier"
        const val KEY_ROLE = "role"
    }
}
