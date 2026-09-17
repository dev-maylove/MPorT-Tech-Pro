package com.mporttech.pro.domain.repository

import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.NetworkDevice
import com.mporttech.pro.domain.model.NetworkInfo

interface NetworkRepository {
    suspend fun getNetworkInfo(): Result<NetworkInfo>
    suspend fun scanDevices(authorized: Boolean): Result<List<NetworkDevice>>
}
