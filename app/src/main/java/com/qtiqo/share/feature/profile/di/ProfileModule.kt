package com.qtiqo.share.feature.profile.di

import com.qtiqo.share.data.prefs.SecureSessionStore
import com.qtiqo.share.data.profile.api.ProfileApi
import com.qtiqo.share.data.profile.repo.FakeProfileRepository
import com.qtiqo.share.data.profile.repo.NetworkProfileRepository
import com.qtiqo.share.data.profile.repo.ProfileRepository
import com.qtiqo.share.data.profile.repo.SwitchingProfileRepository
import com.qtiqo.share.feature.profile.domain.models.ProfileSession
import com.qtiqo.share.feature.profile.integration.SessionManager
import com.qtiqo.share.feature.profile.integration.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import com.qtiqo.share.data.prefs.SettingsStore as AppSettingsStore

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {
    @Provides
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideProfileSessionManager(sessionStore: SecureSessionStore): SessionManager = object : SessionManager {
        override val sessionFlow: Flow<ProfileSession?> = sessionStore.sessionFlow.map { s ->
            s?.let { ProfileSession(userId = it.identifier, email = it.identifier, token = it.token) }
        }

        override suspend fun clearSession() {
            sessionStore.clear()
        }
    }

    @Provides
    @Singleton
    fun provideProfileSettingsStore(settingsStore: AppSettingsStore): SettingsStore = object : SettingsStore {
        override val fakeBackendEnabled = settingsStore.useFakeBackend
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        fake: FakeProfileRepository,
        network: NetworkProfileRepository,
        settingsStore: SettingsStore
    ): ProfileRepository = SwitchingProfileRepository(fake, network, settingsStore)
}
