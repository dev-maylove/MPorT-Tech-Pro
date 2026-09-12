package com.mporttech.pro.data.repository

import com.mporttech.pro.core.database.CustomerDao
import com.mporttech.pro.core.database.CustomerEntity
import com.mporttech.pro.core.database.DiagnosticDao
import com.mporttech.pro.core.database.DiagnosticEntity
import com.mporttech.pro.core.database.TicketDao
import com.mporttech.pro.core.database.TicketEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

class CustomerRepository @Inject constructor(
    private val dao: CustomerDao
) {
    fun observe(): Flow<List<CustomerEntity>> = dao.observeAll()
    suspend fun add(name: String, phone: String, address: String, plan: String) =
        dao.insert(CustomerEntity(name = name, phone = phone, address = address, packageName = plan))
}

class TicketRepository @Inject constructor(
    private val dao: TicketDao
) {
    fun observe(): Flow<List<TicketEntity>> = dao.observeAll()
    suspend fun add(title: String, description: String) =
        dao.insert(TicketEntity(title = title, description = description, customerId = null))
}

class DiagnosticRepository @Inject constructor(
    private val dao: DiagnosticDao
) {
    fun observe(): Flow<List<DiagnosticEntity>> = dao.observeAll()

    /**
     * Reachability that works on Android (ICMP isReachable often fails without root).
     * Strategy: DNS resolve → TCP connect to common ports → optional isReachable fallback.
     */
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
                    // Last resort ICMP (may still fail on many devices)
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
                        ok && usedPort != null -> "Reachable ($ip:$usedPort) ${ms}ms"
                        ok -> "Reachable ($ip) ${ms}ms"
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
