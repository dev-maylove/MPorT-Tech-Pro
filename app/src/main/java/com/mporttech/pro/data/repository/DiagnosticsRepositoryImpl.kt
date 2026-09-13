package com.mporttech.pro.data.repository

import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.PingResult
import com.mporttech.pro.domain.model.PingSample
import com.mporttech.pro.domain.repository.DiagnosticsRepository
import com.mporttech.pro.features.networktools.NetworkOutputParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsRepositoryImpl @Inject constructor() : DiagnosticsRepository {

    override suspend fun ping(host: String, count: Int): Result<PingResult> = try {
        val summary = NetworkOutputParser.ping(host, count)
        Result.Success(
            PingResult(
                host = host,
                sent = summary.transmitted,
                received = summary.received,
                lost = summary.transmitted - summary.received,
                lossPct = summary.lossPct,
                minMs = summary.minMs?.toLong(),
                avgMs = summary.avgMs,
                maxMs = summary.maxMs?.toLong(),
                samples = summary.samples.map {
                    PingSample(
                        seq = it.seq,
                        success = it.success,
                        latencyMs = it.timeMs?.toLong(),
                        message = it.raw
                    )
                }
            )
        )
    } catch (e: Exception) {
        Result.Error(e.message ?: "Ping failed", e)
    }
}
