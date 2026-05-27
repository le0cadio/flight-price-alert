package com.flightpricealert.scheduler

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SchedulerServiceTest {
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        SchedulerService.resetForTests()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        SchedulerService.resetForTests()
        scope.cancel()
    }

    @Test
    fun `scheduler runs worker and stops cleanly`() = runBlocking {
        val executions = AtomicInteger(0)
        val firstExecution = CompletableDeferred<Unit>()

        SchedulerService.start(scope, intervalHours = 1) {
            if (executions.incrementAndGet() == 1) {
                firstExecution.complete(Unit)
            }
        }

        firstExecution.await()
        val beforeStop = executions.get()
        SchedulerService.stop()
        delay(100.milliseconds)

        assertEquals(beforeStop, executions.get())
        assertTrue(SchedulerService.status().contains("running=false"))
    }

    @Test
    fun `ensureRunning restarts scheduler after stop`() = runBlocking {
        val executions = AtomicInteger(0)
        val firstExecution = CompletableDeferred<Unit>()

        SchedulerService.start(scope, intervalHours = 1) {
            if (executions.incrementAndGet() == 1) {
                firstExecution.complete(Unit)
            }
        }
        firstExecution.await()
        SchedulerService.stop()

        SchedulerService.ensureRunning()
        delay(100.milliseconds)
        assertTrue(executions.get() >= 1)
    }

    @Test
    fun `rejects non positive interval`() {
        assertFailsWith<IllegalArgumentException> {
            SchedulerService.start(scope, intervalHours = 0) {}
        }
    }
}

