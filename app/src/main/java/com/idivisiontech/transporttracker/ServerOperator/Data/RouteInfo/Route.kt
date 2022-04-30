package com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo

data class Route(
    val created_at: String,
    val halte: List<Halte>,
    val id: Int,
    val code: String,
    val name: String,
    val updated_at: String
)