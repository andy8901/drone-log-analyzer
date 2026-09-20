package com.neosky.servicesupport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neosky.servicesupport.core.util.ConnectivityObserver
import com.neosky.servicesupport.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-shell level state: who's logged in, and whether we're currently offline. */
@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val currentUser = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val isOffline: StateFlow<Boolean> = connectivityObserver.observe()
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = !connectivityObserver.isCurrentlyOnline(),
        )
}
