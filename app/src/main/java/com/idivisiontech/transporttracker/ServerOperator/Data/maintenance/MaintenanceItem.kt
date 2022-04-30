package com.idivisiontech.transporttracker.ServerOperator.Data.maintenance

data class MaintenanceItem(
    val batas: Int,
    val is_need_action: Boolean,
    val maintenance_history_id: Int,
    val maintenance_name: String,
    val maintenance_id: Int,
    val start_km: Int
)