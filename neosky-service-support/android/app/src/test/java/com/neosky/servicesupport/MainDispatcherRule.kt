package com.neosky.servicesupport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for a [TestDispatcher] for the duration of a test, so
 * `viewModelScope.launch { ... }` (which defaults to `Dispatchers.Main.immediate`) runs
 * eagerly and deterministically under JUnit/Robolectric-free unit tests. Uses
 * [UnconfinedTestDispatcher] (rather than [kotlinx.coroutines.test.StandardTestDispatcher]) so a
 * ViewModel's coroutine runs to its first real suspension point immediately when launched,
 * without every test needing to call `advanceUntilIdle()` after invoking a ViewModel method.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}
