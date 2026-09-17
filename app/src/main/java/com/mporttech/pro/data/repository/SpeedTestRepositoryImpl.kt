package com.mporttech.pro.data.repository

import android.content.Context
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.SpeedTestResult
import com.mporttech.pro.domain.repository.SpeedTestRepository
import com.mporttech.pro.features.speedtest.SpeedTestHistoryStore
import com.mporttech.pro.features.speedtest.SpeedTestRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedTestRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeedTestRepository {

    override suspend fun getHistory(): Result<List<SpeedTestResult>> = try {
        val list = SpeedTestHistoryStore.load(context).map {
            SpeedTestResult(
                timestamp = it.timestamp,
                serverName = it.serverName,
                location = it.location,
                downloadMbps = it.downloadMbps,
                uploadMbps = it.uploadMbps,
                pingMs = it.pingMs,
                jitterMs = it.jitterMs,
                lossPct = it.lossPct
            )
        }
        Result.Success(list)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(e.message ?: "Load history failed", e)
    }

    override suspend fun saveResult(result: SpeedTestResult): Result<Unit> = try {
        SpeedTestHistoryStore.add(
            context,
            SpeedTestRecord(
                timestamp = result.timestamp,
                serverName = result.serverName,
                location = result.location,
                downloadMbps = result.downloadMbps,
                uploadMbps = result.uploadMbps,
                pingMs = result.pingMs,
                jitterMs = result.jitterMs,
                lossPct = result.lossPct
            )
        )
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(e.message ?: "Save failed", e)
    }

    override suspend fun clearHistory(): Result<Unit> = try {
        SpeedTestHistoryStore.clear(context)
        Result.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(e.message ?: "Clear failed", e)
    }
}
