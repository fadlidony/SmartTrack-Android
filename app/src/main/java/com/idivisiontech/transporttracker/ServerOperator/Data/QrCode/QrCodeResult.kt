package com.idivisiontech.transporttracker.ServerOperator.Data.QrCode

data class QrCodeResult(
    val `data`: Data? = null,
    val error: Boolean,
    val message: String
) {
    data class Data(
        val expired_in: Int,
        val image: String
    )
}