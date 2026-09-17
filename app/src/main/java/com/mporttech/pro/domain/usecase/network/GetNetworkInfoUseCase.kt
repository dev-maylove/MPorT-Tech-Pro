package com.mporttech.pro.domain.usecase.network

import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.NetworkInfo
import com.mporttech.pro.domain.repository.NetworkRepository
import javax.inject.Inject

class GetNetworkInfoUseCase @Inject constructor(
    private val repository: NetworkRepository
) {
    suspend operator fun invoke(): Result<NetworkInfo> = repository.getNetworkInfo()
}
