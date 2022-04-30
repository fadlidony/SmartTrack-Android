package com.idivisiontech.transporttracker.ServerOperator.Data.BusInfo

data class Bus(
    val android_id: Int,
    val created_at: String,
    val foto: String,
    val id: Int,
    val name: String,
    val no_voip: String,
    val nomor_mesin: String,
    val nomor_rangka: String,
    val odometer: Int,
    val plat: String,
    val route_id: Int,
    val tahun_pembuatan: Int,
    val updated_at: String
)