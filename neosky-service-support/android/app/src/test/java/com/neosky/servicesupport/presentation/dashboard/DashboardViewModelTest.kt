package com.neosky.servicesupport.presentation.dashboard

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.neosky.servicesupport.MainDispatcherRule
import com.neosky.servicesupport.core.network.ApiException
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.DashboardSummary
import com.neosky.servicesupport.domain.repository.NotificationRepository
import com.neosky.servicesupport.domain.usecase.GetDashboardUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getDashboardUseCase: GetDashboardUseCase = mockk()
    private val notificationRepository: NotificationRepository = mockk {
        every { observeUnreadCount() } returns flowOf(3)
    }

    private val fakeSummary = DashboardSummary(
        registeredDrones = 4,
        activeWarrantyDrones = 3,
        warrantyExpiringSoon = 1,
        openTickets = 2,
        pendingServiceRequests = 1,
        upcomingMaintenance = 1,
        totalFlightHours = 126.5,
        recentFlight = null,
        recentTicket = null,
        recentInvoice = null,
    )

    @Test
    fun `load success populates summary and clears loading`() = runTest {
        coEvery { getDashboardUseCase() } returns NetworkResult.Success(fakeSummary)

        val viewModel = DashboardViewModel(getDashboardUseCase, notificationRepository)

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.summary).isEqualTo(fakeSummary)
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `load failure surfaces the error message and keeps summary null`() = runTest {
        coEvery { getDashboardUseCase() } returns NetworkResult.Error(ApiException.networkError())

        val viewModel = DashboardViewModel(getDashboardUseCase, notificationRepository)

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.summary).isNull()
        assertThat(viewModel.uiState.value.error).isNotNull()
    }

    @Test
    fun `unread notification count is exposed from the notification repository`() = runTest {
        coEvery { getDashboardUseCase() } returns NetworkResult.Success(fakeSummary)

        val viewModel = DashboardViewModel(getDashboardUseCase, notificationRepository)

        viewModel.unreadNotificationCount.test {
            assertThat(awaitItem()).isEqualTo(3)
        }
    }
}
