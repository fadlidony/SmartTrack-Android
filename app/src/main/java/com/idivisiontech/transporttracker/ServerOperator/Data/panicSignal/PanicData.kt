package com.idivisiontech.transporttracker.ServerOperator.Data.panicSignal

data class PanicData(
    val created_at: String,
    val geo_location: String,
    val id: Int,
    val operator_id: Any,
    val session_id: Int,
    val status: String,
    val updated_at: String
)