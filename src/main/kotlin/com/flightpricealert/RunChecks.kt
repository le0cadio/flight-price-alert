package com.flightpricealert

import com.flightpricealert.config.Config
import com.flightpricealert.repository.DatabaseFactory
import com.flightpricealert.service.AppServices
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Entry point used by the GitHub Actions cron workflow: loads config, checks every active
 * alert once, sends emails for good prices, then exits. This replaces running a permanently
 * on server just to host the scheduler.
 */
fun main() {
    val log = LoggerFactory.getLogger("RunChecks")
    val cfg = Config.load()

    DatabaseFactory.init(cfg)
    AppServices.init(cfg)

    log.info("Starting one-shot price check run (environment={})", cfg.appEnvironment)
    runBlocking {
        AppServices.monitorService().checkAllActiveAlerts()
    }
    log.info("Price check run finished")
}
