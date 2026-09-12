package com.mporttech.pro.domain.usecase.speedtest

import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.SpeedTestResult
import com.mporttech.pro.domain.repository.SpeedTestRepository
import javax.inject.Inject

class GetSpeedHistoryUseCase @Inject constructor(
    private val repository: SpeedTestRepository
) {
    suspend operator fun invoke(): Result<List<SpeedTestResult>> = repository.getHistory()
}
