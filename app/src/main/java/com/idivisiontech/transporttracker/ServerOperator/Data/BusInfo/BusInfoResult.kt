package com.idivisiontech.transporttracker.ServerOperator.Data.BusInfo

data class BusInfoResult(
    val bus: Bus?,
    val error: Boolean,
    val message: String
)