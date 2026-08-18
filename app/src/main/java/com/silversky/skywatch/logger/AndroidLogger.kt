package com.silversky.skywatch.logger

import android.util.Log
import com.silversky.core.logger.Logger

class AndroidLogger(
    private val tag: String = "SkyWatch"
) : Logger {

    override fun debug(message: String) {
        Log.d(tag, message)
    }

    override fun info(message: String) {
        Log.i(tag, message)
    }

    override fun warn(message: String) {
        Log.w(tag, message)
    }

    override fun error(message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}