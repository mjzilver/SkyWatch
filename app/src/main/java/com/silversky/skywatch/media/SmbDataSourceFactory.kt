package com.silversky.skywatch.media

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger

@UnstableApi
class SmbDataSourceFactory(
    private val smbClient: SmbClient,
    private val logger: Logger,
) : DataSource.Factory {

  override fun createDataSource(): DataSource {
    return SmbDataSource(
        smbClient = smbClient,
        logger = logger,
    )
  }
}
