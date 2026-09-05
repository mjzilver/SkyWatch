package com.silversky.skywatch.error

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface AppErrorEvent {
  data class ConnectionLost(val serverName: String) : AppErrorEvent

  data class Unhandled(val message: String, val throwable: Throwable? = null) : AppErrorEvent
}

object AppErrorReporter {
  private val _events =
      MutableSharedFlow<AppErrorEvent>(
          replay = 0,
          extraBufferCapacity = 64,
          onBufferOverflow = BufferOverflow.DROP_OLDEST,
      )

  val events: SharedFlow<AppErrorEvent> = _events.asSharedFlow()

  fun report(event: AppErrorEvent) {
    _events.tryEmit(event)
  }
}
