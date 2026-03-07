package com.qtiqo.share.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.qtiqo.share.BuildConfig
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

    private val _useFakeBackend = MutableStateFlow(
        if (BuildConfig.ALLOW_FAKE_BACKEND_TOGGLE) {
            prefs.getBoolean(KEY_FAKE_BACKEND, BuildConfig.DEFAULT_FAKE_BACKEND)
        } else {
            false
        }
    )
    val useFakeBackend: StateFlow<Boolean> = _useFakeBackend.asStateFlow()

    fun setUseFakeBackend(enabled: Boolean) {
        if (!BuildConfig.ALLOW_FAKE_BACKEND_TOGGLE) {
            prefs.edit().putBoolean(KEY_FAKE_BACKEND, false).apply()
            _useFakeBackend.value = false
            return
        }
        prefs.edit().putBoolean(KEY_FAKE_BACKEND, enabled).apply()
        _useFakeBackend.value = enabled
    }

    private companion object {
        const val KEY_FAKE_BACKEND = "use_fake_backend"
    }
}
