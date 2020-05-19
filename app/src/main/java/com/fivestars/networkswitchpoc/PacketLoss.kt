package com.fivestars.networkswitchpoc


object PacketLoss {

    var before: Parse.ParsedNetworkData? = null
    var after: Parse.ParsedNetworkData? = null

    fun calculatePacketLoss(
        beforeData: Parse.ParsedNetworkData,
        afterData: Parse.ParsedNetworkData
    ): Double {
        val rxPackets: Long =
            afterData.rxData.get("packets")!! - beforeData.rxData.get("packets")!!
        val txPackets: Long =
            afterData.txData.get("packets")!! - beforeData.txData.get("packets")!!
        val rxDropped: Long =
            afterData.rxData.get("dropped") as Long - beforeData.rxData.get("dropped")!!
        val txDropped: Long =
            afterData.txData.get("dropped") as Long - beforeData.txData.get("dropped")!!
        return (rxDropped + txDropped + 0.0) / (rxPackets + txPackets) * 100
    }
}
