package com.idivisiontech.transporttracker.Data.Gmap

data class MatrixDistance(
    val destination_addresses: List<String>,
    val origin_addresses: List<String>,
    val rows: List<Row>,
    val status: String
)