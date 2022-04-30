package com.idivisiontech.transporttracker.ServerOperator.Data

data class Profile(
    val avatar: String,
    val created_at: String,
    val deleted_at: Any,
    val file_ktp: String,
    val id: Int,
    val jenis_kelamin: String,
    val name: String,
    val nik: String,
    val rfid_token: String,
    val tanggal_lahir: String,
    val tempat_lahir: String,
    val updated_at: String
)