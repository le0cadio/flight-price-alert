package com.flightpricealert.scheduler

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

object SchedulerService {
    private val log = LoggerFactory.getLogger(SchedulerService::class.java)
    private var job: Job? = null
    private var lastRun: LocalDateTime? = null
    private var startedAt: LocalDateTime? = null
    private var worker: (suspend () -> Unit)? = null
    private var appScope: CoroutineScope? = null
    private var intervalHours: Long = 6
    private val isRunning = AtomicBoolean(false)


    fun start(scope: CoroutineScope, intervalHours: Long = 6, worker: suspend () -> Unit = {}) {
        require(intervalHours > 0) { "intervalHours must be greater than zero" }
        if (job != null || !isRunning.compareAndSet(false, true)) return
        this.appScope = scope
        this.intervalHours = intervalHours
        this.worker = worker
        startedAt = LocalDateTime.now()
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    log.info("Scheduler job started at {}", LocalDateTime.now())
                    this@SchedulerService.worker?.invoke()
                    lastRun = LocalDateTime.now()
                    log.info("Scheduler job completed at {}", LocalDateTime.now())
                } catch (t: Throwable) {
                    log.error("Scheduler run error", t)
                }
                delay((this@SchedulerService.intervalHours * 60 * 60 * 1000).milliseconds)
            }
        }
    }

    fun ensureRunning() {
        if (job != null) return
        val scope = appScope ?: return
        start(scope = scope, intervalHours = intervalHours, worker = worker ?: {})
    }

    fun stop() {
        job?.cancel()
        job = null
        isRunning.set(false)
    }

    fun status(): String {
        val running = job?.isActive == true
        return "running=$running, startedAt=$startedAt, lastRun=$lastRun, intervalHours=$intervalHours"
    }

    internal fun resetForTests() {
        stop()
        lastRun = null
        startedAt = null
        worker = null
        appScope = null
        intervalHours = 6
    }
}

