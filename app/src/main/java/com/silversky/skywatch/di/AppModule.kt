package com.silversky.skywatch.di

import android.content.Context
import androidx.room.Room
import com.silversky.core.logger.Logger
import com.silversky.core.parser.FilenameParser
import com.silversky.core.parser.TokenClassifier
import com.silversky.core.smb.SmbScanner
import com.silversky.skywatch.data.local.PlaybackStateStore
import com.silversky.skywatch.data.local.db.AppDatabase
import com.silversky.skywatch.data.local.db.ScannedMediaDao
import com.silversky.skywatch.data.local.db.SubtitleDao
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
  fun provideServerRepository(
      @ApplicationContext context: Context,
      @ApplicationScope scope: CoroutineScope,
  ): ServerRepository = PersistentServerRepository(context, scope)

  @Provides @Singleton fun provideSmbScanner(): SmbScanner = SmbScanner()

  @Provides
  @Singleton
  fun providePlaybackStateStore(@ApplicationContext context: Context): PlaybackStateStore =
      PlaybackStateStore(context)

  @Provides
  @Singleton
  fun provideSettingsRepository(
      @ApplicationContext context: Context,
      @ApplicationScope scope: CoroutineScope,
  ): SettingsRepository = SettingsRepository(context, scope)

  @Provides
  @Singleton
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
      Room.databaseBuilder(context, AppDatabase::class.java, "skywatch.db")
          .fallbackToDestructiveMigration()
          .build()

  @Provides fun provideSubtitleDao(database: AppDatabase): SubtitleDao = database.subtitleDao()

  @Provides
  fun provideScannedMediaDao(database: AppDatabase): ScannedMediaDao = database.scannedMediaDao()

  @Provides @Singleton fun provideTokenClassifier(): TokenClassifier = TokenClassifier()

  @Provides
  @Singleton
  fun provideFilenameParser(classifier: TokenClassifier): FilenameParser =
      FilenameParser(classifier)
}
