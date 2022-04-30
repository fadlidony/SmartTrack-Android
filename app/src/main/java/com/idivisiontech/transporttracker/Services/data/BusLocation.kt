package com.idivisiontech.transporttracker.Services.data

data class BusLocation(
    val android_id: String = "",
    val jarak_halte: Int = 0,
    val kode_rute: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val nama_halte: String = "",
    val nama_rute: String = "",
    val speed: Double = 0.0,
    val time: Long = 0,
    val waktu_halte: Double = 0.0,
    val nama_bus: String = ""
)