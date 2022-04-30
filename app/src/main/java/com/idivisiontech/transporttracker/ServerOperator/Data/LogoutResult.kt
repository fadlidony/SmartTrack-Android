package com.idivisiontech.transporttracker.ServerOperator.Data

data class LogoutResult(
    val driving_time: Int,
    val error: Boolean,
    val message: String
)