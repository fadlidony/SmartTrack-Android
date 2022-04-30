package com.idivisiontech.transporttracker.RaspberryClient.data

data class HumidityResponse(
    val humidity: Double,
    val success: Boolean,
    val temperature: Double
)