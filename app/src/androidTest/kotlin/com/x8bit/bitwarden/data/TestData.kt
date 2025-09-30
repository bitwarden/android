package com.x8bit.bitwarden.data

import kotlinx.serialization.Serializable

@Serializable
data class TestData(
    val baseUrl: String,
    val email: String,
    val password: String,
)
