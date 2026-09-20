package com.neosky.servicesupport.presentation.flights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.domain.model.Drone
import com.neosky.servicesupport.domain.model.FlightResult
import com.neosky.servicesupport.domain.repository.DroneRepository
import com.neosky.servicesupport.domain.repository.FlightRepository
import com.neosky.servicesupport.domain.usecase.CalculateFlightDurationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class AddFlightLogUiState(
    val droneId: String? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val manualDurationOverride: String = "",
    val location: String = "",
    val maxAltitudeM: String = "",
    val distanceTravelledKm: String = "",
    val missionType: String = "",
    val payloadUsed: String = "",
    val batteryUsed: String = "",
    val batteryCycle: String = "",
    val weather: String = "",
    val flightResult: FlightResult = FlightResult.SUCCESSFUL,
    val remarks: String = "",
    val incidentFlag: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /** The live-computed duration preview shown to the user, honouring a manual override. */
    fun computeDurationMinutes(calculator: CalculateFlightDurationUseCase): Double? {
        manualDurationOverride.toDoubleOrNull()?.let { return it }
        val start = startTime ?: return null
        val end = endTime ?: return null
        return when (val result = calculator(start, end)) {
            is CalculateFlightDurationUseCase.Result.Valid -> result.minutes
            is CalculateFlightDurationUseCase.Result.Invalid -> null
        }
    }
}

@HiltViewModel
class AddFlightLogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    droneRepository: DroneRepository,
    private val flightRepository: FlightRepository,
    private val calculateFlightDurationUseCase: CalculateFlightDurationUseCase,
) : ViewModel() {

    val drones: StateFlow<List<Drone>> = droneRepository.observeDrones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(
        AddFlightLogUiState(droneId = savedStateHandle.get<String>("droneId")?.takeIf { it.isNotBlank() }),
    )
    val uiState: StateFlow<AddFlightLogUiState> = _uiState.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>()
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    /** Reactively recomputed live duration preview shown above the manual-override field. */
    val durationPreviewMinutes: StateFlow<Double?> = _uiState
        .map { it.computeDurationMinutes(calculateFlightDurationUseCase) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onDroneSelected(droneId: String) = update { it.copy(droneId = droneId) }
    fun onStartTimeSelected(instant: Instant) = update { it.copy(startTime = instant) }
    fun onEndTimeSelected(instant: Instant) = update { it.copy(endTime = instant) }
    fun onManualDurationChanged(value: String) = update { it.copy(manualDurationOverride = value) }
    fun onLocationChanged(value: String) = update { it.copy(location = value) }
    fun onMaxAltitudeChanged(value: String) = update { it.copy(maxAltitudeM = value) }
    fun onDistanceChanged(value: String) = update { it.copy(distanceTravelledKm = value) }
    fun onMissionTypeChanged(value: String) = update { it.copy(missionType = value) }
    fun onPayloadUsedChanged(value: String) = update { it.copy(payloadUsed = value) }
    fun onBatteryUsedChanged(value: String) = update { it.copy(batteryUsed = value) }
    fun onBatteryCycleChanged(value: String) = update { it.copy(batteryCycle = value) }
    fun onWeatherChanged(value: String) = update { it.copy(weather = value) }
    fun onFlightResultSelected(result: FlightResult) = update { it.copy(flightResult = result) }
    fun onRemarksChanged(value: String) = update { it.copy(remarks = value) }
    fun onIncidentFlagChanged(value: Boolean) = update { it.copy(incidentFlag = value) }

    private inline fun update(block: (AddFlightLogUiState) -> AddFlightLogUiState) {
        _uiState.value = block(_uiState.value).copy(error = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.droneId.isNullOrBlank()) {
            _uiState.value = state.copy(error = "Please select a drone")
            return
        }
        if (state.startTime == null || state.endTime == null) {
            _uiState.value = state.copy(error = "Please select both a start and end time")
            return
        }
        if (!state.endTime.isAfter(state.startTime)) {
            _uiState.value = state.copy(error = "End time must be after start time")
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            // This is offline-safe by construction: FlightRepository.logFlight writes to Room
            // immediately (PENDING_SYNC) and enqueues background sync — it never makes a
            // network call on this thread, so there is nothing to await or fail here.
            flightRepository.logFlight(
                droneId = state.droneId,
                startTimeIso = state.startTime.toString(),
                endTimeIso = state.endTime.toString(),
                durationMinutes = state.manualDurationOverride.toDoubleOrNull(),
                location = state.location.ifBlank { null },
                maxAltitudeM = state.maxAltitudeM.toDoubleOrNull(),
                distanceTravelledKm = state.distanceTravelledKm.toDoubleOrNull(),
                missionType = state.missionType.ifBlank { null },
                payloadUsed = state.payloadUsed.ifBlank { null },
                batteryUsed = state.batteryUsed.ifBlank { null },
                batteryCycle = state.batteryCycle.toIntOrNull(),
                weather = state.weather.ifBlank { null },
                flightResult = state.flightResult.wireValue,
                remarks = state.remarks.ifBlank { null },
                incidentFlag = state.incidentFlag,
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            _saved.emit(Unit)
        }
    }
}
