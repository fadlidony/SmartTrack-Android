package com.idivisiontech.transporttracker.ServerOperator.Data.maintenance

data class MaintenanceResponse(
        val `data`: List<MaintenanceItem>,
        val error: Boolean
)