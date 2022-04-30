package com.idivisiontech.transporttracker.ServerOperator.Data.CallHelpDeskLog.CallEnd

data class CallEndResult(
    val data: String,
    val error: Boolean,
    val message: String
)