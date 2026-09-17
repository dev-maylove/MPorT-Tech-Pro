package com.mporttech.pro.domain.usecase.network

import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.NetworkDevice
import com.mporttech.pro.domain.repository.NetworkRepository
import javax.inject.Inject

class ScanNetworkUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(authorized: Boolean = true): Result<List<NetworkDevice>> =
        repository.scanDevices(authorized)
}
