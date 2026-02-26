package com.qtiqo.share.feature.profile.integration

import kotlinx.coroutines.flow.Flow

interface SettingsStore {
    val fakeBackendEnabled: Flow<Boolean>
}

