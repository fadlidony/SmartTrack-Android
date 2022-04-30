package com.idivisiontech.transporttracker.Api.osrm.data

data class Waypoint(
    val distance: Double,
    val hint: String,
    val location: List<Double>,
    val name: String
)