package com.idivisiontech.transporttracker.ServerOperator.Data.RouteInfo

data class RouteResult(
    val error: Boolean,
    val message: String,
    val route: Route?
)