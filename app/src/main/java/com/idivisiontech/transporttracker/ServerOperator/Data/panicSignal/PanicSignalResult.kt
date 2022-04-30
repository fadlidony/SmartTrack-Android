package com.idivisiontech.transporttracker.ServerOperator.Data.panicSignal

data class PanicSignalResult(
    val error: Boolean,
    val message: String,
    val panic_data: PanicData? = null
)