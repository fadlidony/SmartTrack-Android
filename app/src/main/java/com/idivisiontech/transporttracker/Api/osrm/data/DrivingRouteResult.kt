package com.idivisiontech.transporttracker.Api.osrm.data

data class DrivingRouteResult(
    var code: String? = null,
    var routes: ArrayList<Route>? = null,
    var waypoints: ArrayList<Waypoint>? = null
)