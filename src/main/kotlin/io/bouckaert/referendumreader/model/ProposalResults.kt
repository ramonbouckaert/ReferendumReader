package io.bouckaert.referendumreader.model

data class ProposalResults(
    val options: List<Option>,
    val formal: Count,
    val informal: Count,
    val total: Count
) {
    data class Option(
        val referendumOptionIdentifier: ReferendumOptionIdentifier,
        var votes: Long
    ) {
        data class ReferendumOptionIdentifier(val id: String, val name: String)
    }
    data class Count(var votes: Long)

    override fun toString(): String {
        val sb = StringBuilder()
        options.forEach { o -> sb.append("${o.referendumOptionIdentifier.name}: ${o.votes}, ") }
        sb.append("Informal: ${informal.votes}, ")
        sb.append("Total: ${total.votes}")
        return sb.toString()
    }
}