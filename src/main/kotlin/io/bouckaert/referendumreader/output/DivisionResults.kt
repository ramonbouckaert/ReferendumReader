package io.bouckaert.referendumreader.output

import kotlinx.serialization.Serializable

@Serializable
data class DivisionResults(
    val id: Int,
    val name: String,
    val state: String,
    val shortCode: String,
    val yes: Long,
    val no: Long,
    val informal: Long,
    val total: Long
)
