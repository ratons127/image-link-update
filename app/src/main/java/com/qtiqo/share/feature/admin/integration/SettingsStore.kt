package com.qtiqo.share.feature.admin.integration

import kotlinx.coroutines.flow.Flow

interface SettingsStore {
    val fakeBackendEnabled: Flow<Boolean>
}
