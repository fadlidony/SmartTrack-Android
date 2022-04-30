package com.idivisiontech.transporttracker.Data


import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Rute(
    val created_at: String,
    val halte: ArrayList<Halte>,
    val id: Int,
    val name: String,
    val updated_at: String
)