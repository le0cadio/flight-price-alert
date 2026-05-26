package com.flightpricealert.repository

import com.flightpricealert.config.AppConfig

object DatabaseFactory {
    @Volatile
    private var initialized = false

    @Volatile
    var activeDbUrl: String = ""
        private set

    fun init(config: AppConfig) {
        if (initialized) return
        activeDbUrl = config.dbUrl
        initialized = true
    }
}



