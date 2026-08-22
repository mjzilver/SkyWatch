package com.silversky.skywatch.di

import android.content.Context
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbScanner
import com.silversky.skywatch.data.local.PlaybackStateStore
import com.silversky.skywatch.data.repository.PersistentServerRepository
import com.silversky.skywatch.data.repository.ServerRepository
import com.silversky.skywatch.data.repository.SettingsRepository
import com.silversky.skywatch.logger.AndroidLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  @ApplicationScope
  fun provideApplicationScope(): CoroutineScope =
      CoroutineScope(SupervisorJob() + Dispatchers.Default)

  @Provides @Singleton fun provideLogger(): Logger = AndroidLogger("SkyWatch")

  @Provides
  @Singleton
  fun provideServerRepository(@ApplicationContext context: Context): ServerRepository =
      PersistentServerRepository(context)

  @Provides @Singleton fun provideSmbScanner(): SmbScanner = SmbScanner()

  @Provides
  @Singleton
  fun providePlaybackStateStore(@ApplicationContext context: Context): PlaybackStateStore =
      PlaybackStateStore(context)

  @Provides
  @Singleton
  fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
      SettingsRepository(context)
}
