package com.mporttech.pro.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Laravel pagination envelope for Resource::collection */
data class PaginatedResponse<T>(
    val data: List<T>? = null,
    val links: Map<String, String?>? = null,
    val meta: PageMeta? = null
)

data class PageMeta(
    val current_page: Int? = null,
    val last_page: Int? = null,
    val per_page: Int? = null,
    val total: Int? = null
)

data class RemoteCustomerDto(
    val id: Long? = null,
    @SerializedName("customer_code") val customerCode: String? = null,
    val name: String? = null,
    val email: String? = null,
    val whatsapp: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val status: String? = null,
    @SerializedName("package_id") val packageId: Long? = null,
    @SerializedName("ip_address") val ipAddress: String? = null,
    val packageX: RemotePackageDto? = null,
    @SerializedName("package") val pkg: RemotePackageDto? = null
)

data class RemotePackageDto(
    val id: Long? = null,
    val name: String? = null
)

data class RemoteTicketDto(
    val id: Long? = null,
    @SerializedName("ticket_number") val ticketNumber: String? = null,
    val subject: String? = null,
    val description: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val category: String? = null,
    @SerializedName("customer_id") val customerId: Long? = null,
    @SerializedName("technician_id") val technicianId: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val customer: RemoteTicketPartyDto? = null,
    val technician: RemoteTicketPartyDto? = null
)

data class RemoteTicketPartyDto(
    val id: Long? = null,
    val name: String? = null,
    @SerializedName("customer_code") val customerCode: String? = null
)

data class CreateTicketRequest(
    val subject: String,
    val description: String,
    val priority: String? = "normal",
    val category: String? = null,
    @SerializedName("customer_id") val customerId: Long? = null
)
