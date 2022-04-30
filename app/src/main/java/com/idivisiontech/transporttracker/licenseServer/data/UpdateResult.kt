package com.idivisiontech.transporttracker.licenseServer.data

data class UpdateResult(
    val created_at: String,
    val file_name: String,
    val file_url: String,
    val id: String,
    val is_head: Boolean,
    val is_skipable: Boolean,
    val updated_at: String,
    val version: String
)