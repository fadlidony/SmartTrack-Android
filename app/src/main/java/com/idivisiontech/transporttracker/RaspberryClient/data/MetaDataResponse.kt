package com.idivisiontech.transporttracker.RaspberryClient.data

data class MetaDataResponse(
    val `data`: Data,
    val success: Boolean
) {
    data class Data(
        val humidity: String,
        val temperature: String,
        val vibration_g: String,
        val vibration_x: String,
        val vibration_y: String,
        val vibration_z: String,
        val door1_status: String,
        val door2_status: String
    )
}