package com.qtiqo.share.data.profile.repo

import com.qtiqo.share.feature.profile.domain.models.ProfileSummary
import com.qtiqo.share.feature.profile.integration.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SwitchingProfileRepository @Inject constructor(
    private val fakeRepo: FakeProfileRepository,
    private val networkRepo: NetworkProfileRepository,
    private val settingsStore: SettingsStore
) : ProfileRepository {
    private suspend fun delegate(): ProfileRepository =
        if (settingsStore.fakeBackendEnabled.first()) fakeRepo else networkRepo

    override suspend fun getSummary(): ProfileSummary = delegate().getSummary()
    override suspend fun changePassword(currentPassword: String, newPassword: String) =
        delegate().changePassword(currentPassword, newPassword)

    override suspend fun logout() = delegate().logout()
}

