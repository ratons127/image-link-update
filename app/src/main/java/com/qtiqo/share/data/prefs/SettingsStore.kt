package com.qtiqo.share.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qtiqo_settings", Context.MODE_PRIVATE)

    private val _useFakeBackend = MutableStateFlow(prefs.getBoolean(KEY_FAKE_BACKEND, true))
    val useFakeBackend: StateFlow<Boolean> = _useFakeBackend.asStateFlow()

    fun setUseFakeBackend(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FAKE_BACKEND, enabled).apply()
        _useFakeBackend.value = enabled
    }

    private companion object {
        const val KEY_FAKE_BACKEND = "use_fake_backend"
    }
}
