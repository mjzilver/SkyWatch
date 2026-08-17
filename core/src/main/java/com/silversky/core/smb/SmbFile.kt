package com.silversky.core.smb

import com.hierynomus.smbj.share.File

interface SmbFile : AutoCloseable {
    val size: Long

    fun read(
        filePosition: Long, buffer: ByteArray, bufferOffset: Int, length: Int
    ): Int
}

class SmbFileImpl(
    private val file: File
) : SmbFile {
    override val size: Long
        get() = file.fileInformation.standardInformation.endOfFile

    override fun read(
        filePosition: Long, buffer: ByteArray, bufferOffset: Int, length: Int
    ): Int {
        return file.read(
            buffer, filePosition, bufferOffset, length
        )
    }

    override fun close() {
        file.close()
    }
}