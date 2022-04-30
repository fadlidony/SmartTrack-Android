package com.idivisiontech.transporttracker.ServerOperator.Data.settingInfo

data class SettingsResult(
    val error: Boolean,
    val message: String,
    val setting_data: ArrayList<SettingData>? = null
)