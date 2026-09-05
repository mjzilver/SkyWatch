package com.silversky.skywatch

import android.app.Application
import com.silversky.skywatch.error.AppErrorEvent
import com.silversky.skywatch.error.AppErrorReporter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SkyWatchApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      AppErrorReporter.report(
          AppErrorEvent.Unhandled(
              message = throwable.message ?: "An unexpected error occurred",
              throwable = throwable,
          )
      )
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }
}
