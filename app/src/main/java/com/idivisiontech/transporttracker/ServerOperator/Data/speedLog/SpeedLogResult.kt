package com.idivisiontech.transporttracker.ServerOperator.Data.speedLog

data class SpeedLogResult(
    val error: Boolean,
    val message: String,
    val speed_data: SpeedData
) {
    data class SpeedData(
        val absensi_id: Int,
        val created_at: String,
        val id: Int,
        val speed: Float,
        val updated_at: String
    )
}