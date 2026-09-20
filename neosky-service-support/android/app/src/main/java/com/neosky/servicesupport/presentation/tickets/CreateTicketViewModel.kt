package com.neosky.servicesupport.presentation.tickets

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.domain.model.TicketCategory
import com.neosky.servicesupport.domain.model.TicketPriority
import com.neosky.servicesupport.domain.repository.DroneRepository
import com.neosky.servicesupport.domain.repository.TicketRepository
import com.neosky.servicesupport.domain.usecase.SubmitTicketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class PendingAttachment(val uri: Uri, val displayName: String, val fileType: String)

data class CreateTicketUiState(
    val droneId: String? = null,
    val category: TicketCategory = TicketCategory.SOFTWARE_ISSUE,
    val subCategory: String = "",
    val priority: TicketPriority = TicketPriority.MEDIUM,
    val subject: String = "",
    val description: String = "",
    val issueDatetime: Instant? = null,
    val location: String = "",
    val flightHoursAtIssue: String = "",
    val attachments: List<PendingAttachment> = emptyList(),
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
)

@HiltViewModel
class CreateTicketViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    droneRepository: DroneRepository,
    private val ticketRepository: TicketRepository,
    private val submitTicketUseCase: SubmitTicketUseCase,
) : ViewModel() {

    val drones: StateFlow<List<Drone>> = droneRepository.observeDrones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(
        CreateTicketUiState(droneId = savedStateHandle.get<String>("droneId")?.takeIf { it.isNotBlank() }),
    )
    val uiState: StateFlow<CreateTicketUiState> = _uiState.asStateFlow()

    private val _submitted = MutableSharedFlow<String>()
    val submitted: SharedFlow<String> = _submitted.asSharedFlow()

    fun onDroneSelected(droneId: String) = update { it.copy(droneId = droneId) }
    fun onCategorySelected(category: TicketCategory) = update { it.copy(category = category) }
    fun onSubCategoryChanged(value: String) = update { it.copy(subCategory = value) }
    fun onPrioritySelected(priority: TicketPriority) = update { it.copy(priority = priority) }
    fun onSubjectChanged(value: String) = update { it.copy(subject = value) }
    fun onDescriptionChanged(value: String) = update { it.copy(description = value) }
    fun onIssueDatetimeSelected(instant: Instant) = update { it.copy(issueDatetime = instant) }
    fun onLocationChanged(value: String) = update { it.copy(location = value) }
    fun onFlightHoursChanged(value: String) = update { it.copy(flightHoursAtIssue = value) }

    fun addAttachment(uri: Uri, displayName: String, fileType: String) {
        update { it.copy(attachments = it.attachments + PendingAttachment(uri, displayName, fileType)) }
    }

    fun removeAttachment(attachment: PendingAttachment) {
        update { it.copy(attachments = it.attachments - attachment) }
    }

    private inline fun update(block: (CreateTicketUiState) -> CreateTicketUiState) {
        _uiState.value = block(_uiState.value).copy(error = null)
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        _uiState.value = state.copy(isSubmitting = true, error = null, fieldErrors = emptyMap())

        viewModelScope.launch {
            val result = submitTicketUseCase(
                droneId = state.droneId.orEmpty(),
                category = state.category.wireValue,
                subCategory = state.subCategory.ifBlank { null },
                priority = state.priority.wireValue,
                subject = state.subject,
                description = state.description,
                issueDatetimeIso = state.issueDatetime?.toString(),
                location = state.location.ifBlank { null },
                flightHoursAtIssue = state.flightHoursAtIssue.toDoubleOrNull(),
            )
            when (result) {
                is NetworkResult.Success -> {
                    uploadAttachments(result.data.id, state.attachments)
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    _submitted.emit(result.data.id)
                }
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    fieldErrors = result.apiException.fieldErrors,
                    error = if (result.apiException.fieldErrors.isEmpty()) result.apiException.message else null,
                )
                NetworkResult.Loading -> Unit
            }
        }
    }

    private suspend fun uploadAttachments(ticketId: String, attachments: List<PendingAttachment>) {
        for (attachment in attachments) {
            val file = copyUriToCacheFile(attachment.uri, attachment.displayName) ?: continue
            ticketRepository.addAttachment(ticketId, file, attachment.fileType)
        }
    }

    private fun copyUriToCacheFile(uri: Uri, displayName: String): File? = try {
        val cacheFile = File(appContext.cacheDir, "${UUID.randomUUID()}-$displayName")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        }
        cacheFile
    } catch (t: Throwable) {
        null
    }
}
