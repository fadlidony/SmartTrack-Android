package com.idivisiontech.transporttracker.ServerOperator.Data

data class LoginResult(
    val error: Boolean,
    val key: String,
    val message: String
)