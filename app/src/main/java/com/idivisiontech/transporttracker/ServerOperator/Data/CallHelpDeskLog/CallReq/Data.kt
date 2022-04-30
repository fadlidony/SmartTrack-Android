package com.idivisiontech.transporttracker.ServerOperator.Data.CallHelpDeskLog.CallReq

data class Data(
    val call_at: String,
    val created_at: String,
    val id: Int,
    val session_id: Int,
    val updated_at: String
)