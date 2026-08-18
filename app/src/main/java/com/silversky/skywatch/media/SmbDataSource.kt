package com.silversky.skywatch.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.silversky.core.client.SmbClient
import com.silversky.core.smb.SmbFile

class SmbDataSource(
    private val smbClient: SmbClient
) : BaseDataSource(false) {

    private var file: SmbFile? = null
    private var currentUri: Uri? = null

    private var position = 0L
    private var remaining = 0L

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)

        currentUri = dataSpec.uri

        val shareName = dataSpec.uri.host
            ?: throw IllegalArgumentException(
                "SMB URI has no share name"
            )

        val path = dataSpec.uri.path
            ?.trimStart('/')
            ?.replace('/', '\\')
            ?: throw IllegalArgumentException(
                "SMB URI has no path"
            )

        val openedFile = smbClient.openFile(
            shareName = shareName,
            path = path
        ) ?: throw IllegalStateException(
            "File does not exist: $shareName/$path"
        )

        file = openedFile

        position = dataSpec.position

        remaining =
            if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                openedFile.size - position
            } else {
                dataSpec.length
            }

        transferStarted(dataSpec)

        return remaining
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        if (length == 0) {
            return 0
        }

        if (remaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val requested = minOf(
            length.toLong(),
            remaining
        ).toInt()

        val bytesRead = file?.read(
            filePosition = position,
            buffer = buffer,
            bufferOffset = offset,
            length = requested
        ) ?: throw IllegalStateException(
            "SMB file is not open"
        )

        if (bytesRead <= 0) {
            return C.RESULT_END_OF_INPUT
        }

        position += bytesRead
        remaining -= bytesRead

        bytesTransferred(bytesRead)

        return bytesRead
    }

    override fun getUri() = currentUri

    override fun close() {
        file?.close()
        file = null

        transferEnded()
    }
}