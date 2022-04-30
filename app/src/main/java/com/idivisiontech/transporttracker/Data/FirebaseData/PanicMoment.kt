package com.idivisiontech.transporttracker.Data.FirebaseData

class PanicMoment(
        var fb_id:String,
        var timestamp:Long,
        var panic_id:Int,
        var state:String,
        var message:String
) {
    constructor() : this("",-1, -1,"PENDING","") {

    }
}