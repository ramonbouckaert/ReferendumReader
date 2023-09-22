package io.bouckaert.referendumreader

data class AppConfig(
    val pollingFrequency: Long,
    val aec: Aec
) {
    data class Aec (
        val host: String,
        val port: Int?,
        val electoralEvent: Long
    )
}