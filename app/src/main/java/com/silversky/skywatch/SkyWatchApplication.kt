package com.silversky.skywatch

import android.app.Application
import com.silversky.skywatch.error.AppErrorEvent.Unhandled
import com.silversky.skywatch.error.AppErrorReporter
import com.silversky.skywatch.error.toAppError
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SkyWatchApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      AppErrorReporter.report(Unhandled(throwable.toAppError()))
      defaultHandler?.uncaughtException(thread, throwable)
    }
  }
}
