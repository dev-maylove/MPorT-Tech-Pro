package com.mporttech.pro.domain.usecase.diagnostics

import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.PingResult
import com.mporttech.pro.domain.repository.DiagnosticsRepository
import javax.inject.Inject

class PingUseCase @Inject constructor(
    private val repository: DiagnosticsRepository
) {
    suspend operator fun invoke(host: String, count: Int = 4): Result<PingResult> =
        repository.ping(host, count)
}
