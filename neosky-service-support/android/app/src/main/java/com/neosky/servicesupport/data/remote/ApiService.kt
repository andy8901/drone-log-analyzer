package com.neosky.servicesupport.data.remote

import com.neosky.servicesupport.data.remote.dto.AddCommentRequestDto
import com.neosky.servicesupport.data.remote.dto.CloseTicketRequestDto
import com.neosky.servicesupport.data.remote.dto.CreateFlightRequestDto
import com.neosky.servicesupport.data.remote.dto.CreateTicketRequestDto
import com.neosky.servicesupport.data.remote.dto.CustomerDto
import com.neosky.servicesupport.data.remote.dto.DashboardDto
import com.neosky.servicesupport.data.remote.dto.DroneComponentDto
import com.neosky.servicesupport.data.remote.dto.DroneDto
import com.neosky.servicesupport.data.remote.dto.DocumentDto
import com.neosky.servicesupport.data.remote.dto.FcmTokenRequestDto
import com.neosky.servicesupport.data.remote.dto.FlightLogDto
import com.neosky.servicesupport.data.remote.dto.FlightStatsDto
import com.neosky.servicesupport.data.remote.dto.ForgotPasswordRequestDto
import com.neosky.servicesupport.data.remote.dto.InvoiceDto
import com.neosky.servicesupport.data.remote.dto.LoginRequestDto
import com.neosky.servicesupport.data.remote.dto.LoginResponseDto
import com.neosky.servicesupport.data.remote.dto.MaintenanceInfoDto
import com.neosky.servicesupport.data.remote.dto.MessageResponseDto
import com.neosky.servicesupport.data.remote.dto.NotificationDto
import com.neosky.servicesupport.data.remote.dto.PageDto
import com.neosky.servicesupport.data.remote.dto.RefreshRequestDto
import com.neosky.servicesupport.data.remote.dto.RefreshResponseDto
import com.neosky.servicesupport.data.remote.dto.RegisterRequestDto
import com.neosky.servicesupport.data.remote.dto.RegisterResponseDto
import com.neosky.servicesupport.data.remote.dto.ResetPasswordRequestDto
import com.neosky.servicesupport.data.remote.dto.SearchResultsDto
import com.neosky.servicesupport.data.remote.dto.ServiceRecordDto
import com.neosky.servicesupport.data.remote.dto.UpdateProfileRequestDto
import com.neosky.servicesupport.data.remote.dto.VerifyOtpRequestDto
import com.neosky.servicesupport.data.remote.dto.VerifyOtpResponseDto
import com.neosky.servicesupport.data.remote.dto.WarrantyDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Retrofit surface for every endpoint a `customer`-role user calls, mirroring
 * `docs/API_SPEC.md` §§1-13 exactly. Admin/engineer-only endpoints (§14) are out of scope for
 * this customer-facing app and are intentionally not declared here.
 */
interface ApiService {

    // ---------------------------------------------------------------- Auth
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): RegisterResponseDto

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequestDto): VerifyOtpResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): RefreshResponseDto

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): MessageResponseDto

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): MessageResponseDto

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // ---------------------------------------------------------- Customer profile
    @GET("customer/profile")
    suspend fun getProfile(): CustomerDto

    @PUT("customer/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequestDto): CustomerDto

    @PUT("customer/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenRequestDto): Response<Unit>

    // ---------------------------------------------------------------- Dashboard
    @GET("dashboard")
    suspend fun getDashboard(): DashboardDto

    // ---------------------------------------------------------------------- Drones
    @GET("drones")
    suspend fun getDrones(@Query("status") status: String? = null): PageDto<DroneDto>

    @GET("drones/{id}")
    suspend fun getDrone(@Path("id") droneId: String): DroneDto

    @GET("drones/{id}/components")
    suspend fun getDroneComponents(@Path("id") droneId: String): List<DroneComponentDto>

    @GET("drones/search")
    suspend fun searchDrones(@Query("q") query: String): List<DroneDto>

    // --------------------------------------------------------------------- Tickets
    @GET("tickets")
    suspend fun getTickets(
        @Query("status") status: String? = null,
        @Query("drone_id") droneId: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): PageDto<com.neosky.servicesupport.data.remote.dto.TicketDto>

    @GET("tickets/{id}")
    suspend fun getTicket(@Path("id") ticketId: String): com.neosky.servicesupport.data.remote.dto.TicketDto

    @POST("tickets")
    suspend fun createTicket(@Body body: CreateTicketRequestDto): com.neosky.servicesupport.data.remote.dto.TicketDto

    @POST("tickets/{id}/comments")
    suspend fun addTicketComment(
        @Path("id") ticketId: String,
        @Body body: AddCommentRequestDto,
    ): com.neosky.servicesupport.data.remote.dto.TicketCommentDto

    @Multipart
    @POST("tickets/{id}/attachments")
    suspend fun addTicketAttachment(
        @Path("id") ticketId: String,
        @Part file: MultipartBody.Part,
        @Part("file_type") fileType: RequestBody,
    ): com.neosky.servicesupport.data.remote.dto.TicketAttachmentDto

    @POST("tickets/{id}/close")
    suspend fun closeTicket(
        @Path("id") ticketId: String,
        @Body body: CloseTicketRequestDto,
    ): com.neosky.servicesupport.data.remote.dto.TicketDto

    // --------------------------------------------------------------------- Flights
    @GET("flights")
    suspend fun getFlights(
        @Query("drone_id") droneId: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): PageDto<FlightLogDto>

    @GET("flights/stats")
    suspend fun getFlightStats(): FlightStatsDto

    @POST("flights")
    suspend fun createFlight(@Body body: CreateFlightRequestDto): FlightLogDto

    @PUT("flights/{id}")
    suspend fun updateFlight(@Path("id") flightId: String, @Body body: CreateFlightRequestDto): FlightLogDto

    @DELETE("flights/{id}")
    suspend fun deleteFlight(@Path("id") flightId: String): Response<Unit>

    // -------------------------------------------------------------------- Warranty
    @GET("drones/{id}/warranty")
    suspend fun getWarranty(@Path("id") droneId: String): WarrantyDto

    // ----------------------------------------------------------------- Maintenance
    @GET("drones/{id}/maintenance")
    suspend fun getMaintenance(@Path("id") droneId: String): MaintenanceInfoDto

    // -------------------------------------------------------------------- Invoices
    @GET("invoices")
    suspend fun getInvoices(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
    ): PageDto<InvoiceDto>

    @GET("invoices/{id}")
    suspend fun getInvoice(@Path("id") invoiceId: String): InvoiceDto

    @Streaming
    @GET("invoices/{id}/pdf")
    suspend fun getInvoicePdf(@Path("id") invoiceId: String): Response<ResponseBody>

    // --------------------------------------------------------------- Service history
    @GET("drones/{id}/service-history")
    suspend fun getServiceHistory(@Path("id") droneId: String): List<ServiceRecordDto>

    // -------------------------------------------------------------------- Documents
    @GET("documents")
    suspend fun getDocuments(
        @Query("drone_id") droneId: String? = null,
        @Query("ticket_id") ticketId: String? = null,
        @Query("type") type: String? = null,
    ): List<DocumentDto>

    @Streaming
    @GET("documents/{id}/download")
    suspend fun downloadDocument(@Path("id") documentId: String): Response<ResponseBody>

    // ---------------------------------------------------------------- Notifications
    @GET("notifications")
    suspend fun getNotifications(
        @Query("unread_only") unreadOnly: Boolean? = null,
        @Query("page") page: Int = 1,
    ): PageDto<NotificationDto>

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") notificationId: String): Response<Unit>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>

    // --------------------------------------------------------------------- Search
    @GET("search")
    suspend fun search(@Query("q") query: String): SearchResultsDto
}
