package com.idivisiontech.transporttracker.ServerOperator.Data.maintenance

data class SensorResponse(
        val `data`: List<SensorItem>,
        val error: Boolean
)