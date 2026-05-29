package io.github.soundremote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

// https://developer.android.com/kotlin/coroutines/test#setting-main-dispatcher
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(p0: ExtensionContext) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(p0: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
