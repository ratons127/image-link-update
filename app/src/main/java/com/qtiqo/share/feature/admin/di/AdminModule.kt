package com.qtiqo.share.feature.admin.di

import com.qtiqo.share.data.prefs.SecureSessionStore
import com.qtiqo.share.feature.admin.data.api.AdminApi
import com.qtiqo.share.feature.admin.data.repo.AdminRepository
import com.qtiqo.share.feature.admin.data.repo.FakeAdminRepository
import com.qtiqo.share.feature.admin.data.repo.NetworkAdminRepository
import com.qtiqo.share.feature.admin.data.repo.SwitchingAdminRepository
import com.qtiqo.share.feature.admin.integration.Session
import com.qtiqo.share.feature.admin.integration.SessionManager
import com.qtiqo.share.feature.admin.integration.SettingsStore
import com.qtiqo.share.domain.model.UserRole
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
object AdminModule {
    @Provides
    fun provideAdminApi(retrofit: Retrofit): AdminApi = retrofit.create(AdminApi::class.java)

    @Provides
    @Singleton
    fun provideAdminSessionManager(store: SecureSessionStore): SessionManager = object : SessionManager {
        override val sessionFlow: Flow<Session?> = store.sessionFlow.map { s ->
            s?.let {
                Session(
                    userId = it.identifier,
                    email = it.identifier,
                    role = if (it.role == UserRole.ADMIN) "ADMIN" else "USER",
                    token = it.token
                )
            }
        }
    }

    @Provides
    @Singleton
    fun provideAdminSettingsStore(settingsStore: AppSettingsStore): SettingsStore = object : SettingsStore {
        override val fakeBackendEnabled = settingsStore.useFakeBackend
    }

    @Provides
    @Singleton
    fun provideAdminRepository(
        fakeRepo: FakeAdminRepository,
        networkRepo: NetworkAdminRepository,
        settingsStore: SettingsStore
    ): AdminRepository = SwitchingAdminRepository(fakeRepo, networkRepo, settingsStore)
}
