package com.mporttech.pro.domain.repository

import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.PingResult

interface DiagnosticsRepository {
    suspend fun ping(host: String, count: Int = 4): Result<PingResult>
}
