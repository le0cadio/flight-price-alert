@file:Suppress("unused")

package com.flightpricealert.api

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse<T> {
    @Serializable
    @Suppress("unused")
    data class Success<T>(
        val data: T,
        val message: String = "Success"
    ) : ApiResponse<T>()

    @Serializable
    @Suppress("unused")
    data class Error<T>(
        val error: String,
        val details: String? = null,
        val timestamp: String = java.time.LocalDateTime.now().toString()
    ) : ApiResponse<T>()
}

