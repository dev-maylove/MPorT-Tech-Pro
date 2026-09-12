package com.mporttech.pro.domain.repository

import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.SpeedTestResult

interface SpeedTestRepository {
    suspend fun getHistory(): Result<List<SpeedTestResult>>
    suspend fun saveResult(result: SpeedTestResult): Result<Unit>
    suspend fun clearHistory(): Result<Unit>
}
