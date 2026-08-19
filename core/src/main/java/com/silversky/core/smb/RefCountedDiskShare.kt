package com.silversky.core.smb

import com.hierynomus.smbj.share.DiskShare

class RefCountedDiskShare(
    val shareName: String,
    val share: DiskShare,
) : AutoCloseable {

  private val lock = Any()

  private var references = 0
  private var closed = false

  fun acquire() {
    synchronized(lock) {
      check(!closed) {
        "DiskShare '$shareName' has already been closed"
      }

      references++
    }
  }

  fun release() {
    synchronized(lock) {
      if (references <= 0) {
        return
      }

      references--

      if (references == 0) {
        closeLocked()
      }
    }
  }

  fun referenceCount(): Int {
    synchronized(lock) {
      return references
    }
  }

  fun isClosed(): Boolean {
    synchronized(lock) {
      return closed
    }
  }

  override fun close() {
    synchronized(lock) {
      closeLocked()
    }
  }

  private fun closeLocked() {
    if (closed) {
      return
    }

    closed = true

    try {
      share.close()
    } catch (_: Exception) {}
  }
}
