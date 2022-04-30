package com.idivisiontech.transporttracker.Api.osrm.data

data class Leg(
    val distance: Double,
    val duration: Double,
    val steps: List<Any>,
    val summary: String,
    val weight: Double
)