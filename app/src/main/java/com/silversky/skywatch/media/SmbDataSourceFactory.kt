package com.silversky.skywatch.media

import androidx.media3.datasource.DataSource
import com.silversky.core.client.SmbClient

class SmbDataSourceFactory(
    private val smbClient: SmbClient
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return SmbDataSource(smbClient)
    }
}