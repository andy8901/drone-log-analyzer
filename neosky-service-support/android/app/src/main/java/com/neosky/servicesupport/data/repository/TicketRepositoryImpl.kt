package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.data.local.dao.TicketDao
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.data.remote.dto.AddCommentRequestDto
import com.neosky.servicesupport.data.remote.dto.CloseTicketRequestDto
import com.neosky.servicesupport.data.remote.dto.CreateTicketRequestDto
import com.neosky.servicesupport.domain.model.Ticket
import com.neosky.servicesupport.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TicketRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val ticketDao: TicketDao,
) : TicketRepository {

    override fun observeTickets(): Flow<List<Ticket>> = ticketDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshTickets(status: String?, droneId: String?, page: Int): NetworkResult<List<Ticket>> {
        val result = safeApiCall { apiService.getTickets(status = status, droneId = droneId, page = page) }
        return when (result) {
            is NetworkResult.Success -> {
                val dtos = result.data.items
                ticketDao.upsertAll(dtos.map { it.toEntity() })
                NetworkResult.Success(dtos.map { it.toDomain() })
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override fun observeTicket(ticketId: String): Flow<Ticket?> = ticketDao.observeById(ticketId).map { it?.toDomain() }

    override suspend fun refreshTicket(ticketId: String): NetworkResult<Ticket> {
        val result = safeApiCall { apiService.getTicket(ticketId) }
        return when (result) {
            is NetworkResult.Success -> {
                ticketDao.upsert(result.data.toEntity())
                NetworkResult.Success(result.data.toDomain())
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun createTicket(
        droneId: String,
        category: String,
        subCategory: String?,
        priority: String,
        subject: String,
        description: String,
        issueDatetimeIso: String?,
        location: String?,
        flightHoursAtIssue: Double?,
    ): NetworkResult<Ticket> {
        val result = safeApiCall {
            apiService.createTicket(
                CreateTicketRequestDto(
                    droneId = droneId,
                    category = category,
                    subCategory = subCategory,
                    priority = priority,
                    subject = subject,
                    description = description,
                    issueDatetime = issueDatetimeIso,
                    location = location,
                    flightHoursAtIssue = flightHoursAtIssue,
                ),
            )
        }
        return when (result) {
            is NetworkResult.Success -> {
                ticketDao.upsert(result.data.toEntity())
                NetworkResult.Success(result.data.toDomain())
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun addComment(ticketId: String, comment: String): NetworkResult<Unit> {
        val result = safeApiCall { apiService.addTicketComment(ticketId, AddCommentRequestDto(comment)) }
        return when (result) {
            is NetworkResult.Success -> {
                refreshTicket(ticketId) // keep the cached timeline/comments in sync
                NetworkResult.Success(Unit)
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun addAttachment(ticketId: String, file: File, fileType: String): NetworkResult<Unit> {
        val mimeType = when (fileType) {
            "photo" -> "image/*"
            "video" -> "video/*"
            else -> "application/octet-stream"
        }
        val requestFile = file.asRequestBody(mimeType.toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val fileTypeBody = fileType.toRequestBody("text/plain".toMediaType())

        val result = safeApiCall { apiService.addTicketAttachment(ticketId, filePart, fileTypeBody) }
        return when (result) {
            is NetworkResult.Success -> {
                refreshTicket(ticketId)
                NetworkResult.Success(Unit)
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun closeTicket(ticketId: String, feedback: String?): NetworkResult<Unit> {
        val result = safeApiCall { apiService.closeTicket(ticketId, CloseTicketRequestDto(confirmed = true, feedback = feedback)) }
        return when (result) {
            is NetworkResult.Success -> {
                ticketDao.upsert(result.data.toEntity())
                NetworkResult.Success(Unit)
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }
}
