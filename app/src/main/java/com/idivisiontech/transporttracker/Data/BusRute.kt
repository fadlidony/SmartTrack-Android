package com.idivisiontech.transporttracker.Data

import com.google.firebase.database.Exclude

data class BusRute (
        var rute_id: Int
){


    @Exclude
    fun toMap() : Map<String, Any?>{
        return mapOf(
                "rute_id" to rute_id
        )
    }


}