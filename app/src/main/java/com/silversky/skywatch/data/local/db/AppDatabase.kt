package com.silversky.skywatch.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaEntity::class, SubtitleEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
  abstract fun subtitleDao(): SubtitleDao
}
