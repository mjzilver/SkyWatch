package com.silversky.skywatch.error

import androidx.media3.common.PlaybackException

data class AppError(
    val userMessage: String,
    val technicalMessage: String,
)

fun PlaybackException.toAppError(): AppError {
  val userMessage =
      when (errorCode) {
        PlaybackException.ERROR_CODE_DECODING_FAILED ->
            "This video uses a format that your device cannot decode."
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
            "This video format is not supported by your device."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
            "Failed to initialize the video or audio decoder. Your device may not support this specific codec."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
            "The video file appears to be damaged or uses an unsupported container."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
            "The network connection to the server was lost."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "The connection to the server timed out."
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "The requested file was not found on the server."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
            "The server returned an invalid response."
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
            "An error occurred while reading the file from the server."
        PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
        PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR,
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED ->
            "This content is protected and cannot be played on this device."
        else -> "An unexpected playback error occurred."
      }

  val technicalDetails =
      StringBuilder()
          .apply {
            append("Error: $errorCodeName ($errorCode)")
            message?.let { append("\n$it") }
            cause?.let {
              append("\nCaused by: ${it.message ?: it.javaClass.simpleName}")
            }
          }
          .toString()

  return AppError(userMessage, technicalDetails)
}

fun Throwable.toAppError(userMessage: String = "An unexpected error occurred"): AppError {
  val technicalDetails =
      StringBuilder()
          .apply {
            append(this@toAppError.javaClass.simpleName)
            message?.let { append(": $it") }
            cause?.let {
              append("\nCaused by: ${it.message ?: it.javaClass.simpleName}")
            }
          }
          .toString()

  return AppError(userMessage, technicalDetails)
}
