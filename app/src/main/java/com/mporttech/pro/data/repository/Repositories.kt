package com.mporttech.pro.data.repository

import com.mporttech.pro.core.common.NetworkErrors
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.core.database.CustomerDao
import com.mporttech.pro.core.database.CustomerEntity
import com.mporttech.pro.core.database.DiagnosticDao
import com.mporttech.pro.core.database.DiagnosticEntity
import com.mporttech.pro.core.database.TicketDao
import com.mporttech.pro.core.database.TicketEntity
import com.mporttech.pro.data.remote.MportApi
import com.mporttech.pro.data.remote.dto.CreateTicketRequest
import com.mporttech.pro.data.remote.dto.RemoteCustomerDto
import com.mporttech.pro.data.remote.dto.RemoteTicketDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton


/** Parse Laravel/ISO timestamps; falls back to now on failure. */
private fun parseRemoteTimestamp(raw: String?): Long {
    if (raw.isNullOrBlank()) return System.currentTimeMillis()
    val t = raw.trim()
    // Instant.parse handles "...Z" and offset forms
    try {
        return java.time.Instant.parse(t).toEpochMilli()
    } catch (_: Exception) { }
    // "2024-01-01 12:00:00" or with fractional seconds
    val normalized = t.replace(' ', 'T').let { s ->
        when {
            s.endsWith("Z") || s.contains('+') || Regex("""[+-]\d{2}:\d{2}$""").containsMatchIn(s) -> s
            else -> s + "Z"
        }
    }
    try {
        return java.time.Instant.parse(normalized).toEpochMilli()
    } catch (_: Exception) { }
    // epoch seconds / millis
    t.toLongOrNull()?.let { n ->
        return if (n < 10_000_000_000L) n * 1000L else n
    }
    return System.currentTimeMillis()
}

@Singleton
class CustomerRepository @Inject constructor(
    private val dao: CustomerDao,
    private val api: MportApi
) {
    fun observe(): Flow<List<CustomerEntity>> = dao.observeAll()

    suspend fun add(name: String, phone: String, address: String, plan: String) =
        dao.insert(
            CustomerEntity(
                name = name,
                phone = phone,
                address = address,
                packageName = plan
            )
        )

    /** Pull customers from Laravel and replace local cache. */
    @Volatile var lastSyncAtMs: Long = 0L
        private set

    suspend fun syncFromRemote(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = api.customers(page = 1)
            if (!response.isSuccessful) {
                return@withContext Result.Error(NetworkErrors.fromHttpCode(response.code()))
            }
            val list = response.body()?.data.orEmpty()
            val entities = list.map { it.toEntity() }
            // Replace atomically so observers never see a partially synced cache.
            dao.replaceAll(entities)
            lastSyncAtMs = System.currentTimeMillis()
            Result.Success(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep existing local data on failure
            Result.Error(NetworkErrors.userMessage(e))
        }
    }

    private fun RemoteCustomerDto.toEntity() = CustomerEntity(
        remoteId = id,
        customerCode = customerCode.orEmpty(),
        name = name.orEmpty(),
        phone = whatsapp ?: phone.orEmpty(),
        email = email.orEmpty(),
        address = address.orEmpty(),
        packageName = pkg?.name ?: packageX?.name.orEmpty(),
        status = status ?: "active",
        ipAddress = ipAddress.orEmpty()
    )
}

@Singleton
class TicketRepository @Inject constructor(
    private val dao: TicketDao,
    private val api: MportApi
) {
    @Volatile var lastSyncAtMs: Long = 0L
        private set
    fun observe(): Flow<List<TicketEntity>> = dao.observeAll()

    suspend fun add(title: String, description: String) {
        // Prefer server create when online
        try {
            val response = api.createTicket(
                CreateTicketRequest(subject = title, description = description)
            )
            if (response.isSuccessful) {
                val remote = response.body()
                if (remote != null) {
                    dao.insert(remote.toEntity())
                    return
                }
            }
        } catch (_: Exception) {
            // fall through to local
        }
        dao.insert(TicketEntity(title = title, description = description, customerId = null))
    }

    suspend fun updateStatus(item: TicketEntity, status: String) =
        dao.update(item.copy(status = status))

    /** Pull tickets from Laravel; only replace local cache after success. */
    suspend fun syncFromRemote(status: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = api.tickets(status = status, page = 1)
            if (!response.isSuccessful) {
                return@withContext Result.Error(NetworkErrors.fromHttpCode(response.code()))
            }
            val list = response.body()?.data.orEmpty()
            val entities = list.map { it.toEntity() }
            dao.replaceAll(entities)
            lastSyncAtMs = System.currentTimeMillis()
            Result.Success(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(NetworkErrors.userMessage(e))
        }
    }

    private fun RemoteTicketDto.toEntity(): TicketEntity {
        val createdMs = parseRemoteTimestamp(createdAt)
        return TicketEntity(
            remoteId = id,
            ticketNumber = ticketNumber.orEmpty(),
            customerId = customerId,
            title = subject.orEmpty(),
            description = description.orEmpty(),
            status = (status ?: "OPEN").uppercase(),
            priority = priority ?: "normal",
            category = category.orEmpty(),
            customerName = customer?.name.orEmpty(),
            technicianName = technician?.name.orEmpty(),
            createdAt = createdMs
        )
    }
}

class DiagnosticRepository @Inject constructor(
    private val dao: DiagnosticDao
) {
    fun observe(): Flow<List<DiagnosticEntity>> = dao.observeAll()

    suspend fun ping(target: String): DiagnosticEntity = withContext(Dispatchers.IO) {
        val host = target.trim()
        val start = System.nanoTime()
        val entity = try {
            if (host.isEmpty()) {
                DiagnosticEntity(target = host, success = false, latencyMs = null, message = "Host kosong")
            } else {
                val address = try {
                    InetAddress.getByName(host)
                } catch (e: Exception) {
                    return@withContext DiagnosticEntity(
                        target = host,
                        success = false,
                        latencyMs = null,
                        message = "DNS gagal: ${e.message ?: "unknown host"}"
                    ).also { dao.insert(it) }
                }

                val ip = address.hostAddress ?: host
                val ports = intArrayOf(443, 80, 53, 22, 8080, 8728)
                var ok = false
                var usedPort: Int? = null
                for (port in ports) {
                    try {
                        Socket().use { socket ->
                            socket.tcpNoDelay = true
                            socket.connect(InetSocketAddress(address, port), 1200)
                            ok = true
                            usedPort = port
                        }
                        if (ok) break
                    } catch (_: Exception) {
                    }
                }
                if (!ok) {
                    ok = try {
                        address.isReachable(1500)
                    } catch (_: Exception) {
                        false
                    }
                }
                val ms = (System.nanoTime() - start) / 1_000_000
                DiagnosticEntity(
                    target = host,
                    success = ok,
                    latencyMs = if (ok) ms else null,
                    message = when {
                        ok && usedPort != null -> "Reachable/Terjangkau ($ip:$usedPort) ${ms} ms"
                        ok -> "Reachable/Terjangkau ($ip) ${ms} ms"
                        else -> "No response ($ip) — cek jaringan/firewall"
                    }
                )
            }
        } catch (e: Exception) {
            DiagnosticEntity(
                target = host,
                success = false,
                latencyMs = null,
                message = e.message ?: "Error"
            )
        }
        dao.insert(entity)
        entity
    }
}
