package com.silversky.skywatch.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MediaEntity::class, SubtitleEntity::class, ScannedMediaEntity::class],
    version = 3,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun subtitleDao(): SubtitleDao

  abstract fun scannedMediaDao(): ScannedMediaDao
}
