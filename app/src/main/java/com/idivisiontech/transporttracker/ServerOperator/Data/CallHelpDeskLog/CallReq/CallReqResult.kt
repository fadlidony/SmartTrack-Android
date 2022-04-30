package com.idivisiontech.transporttracker.ServerOperator.Data.CallHelpDeskLog.CallReq

data class CallReqResult(
    val data: Data,
    val error: Boolean,
    val message: String
)