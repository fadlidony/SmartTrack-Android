package com.idivisiontech.transporttracker.Api.osrm.data

data class Route(
    val distance: Double,
    val duration: Double,
    val geometry: String,
    val legs: List<Leg>,
    val weight: Double,
    val weight_name: String
)