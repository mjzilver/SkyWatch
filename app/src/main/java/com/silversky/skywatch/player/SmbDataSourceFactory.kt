package com.silversky.skywatch.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.TransferListener
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbClient

@UnstableApi
class SmbDataSourceFactory(
    private val smbClient: SmbClient,
    private val logger: Logger,
    private val transferListener: TransferListener? = null,
) : DataSource.Factory {

  override fun createDataSource(): DataSource {
    val dataSource =
        SmbDataSource(
            smbClient = smbClient,
            logger = logger,
        )
    transferListener?.let { dataSource.addTransferListener(it) }
    return dataSource
  }
}
