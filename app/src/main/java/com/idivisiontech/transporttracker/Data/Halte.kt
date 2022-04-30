package com.idivisiontech.transporttracker.Data

data class Halte(
    val arrived_greeting: String,
    val arriving_announcement: String,
    val created_at: String,
    val going_to_announcement: String,
    val id: Int,
    val lang: String,
    val lat: String,
    val name: String,
    val pivot: Pivot,
    val updated_at: String
)