package io.bouckaert.referendumreader.output

import kotlinx.serialization.*
@Serializable
data class BoothResults(
    val id: Int,
    val name: String,
    val yes: Long,
    val no: Long,
    val informal: Long,
    val total: Long
)
