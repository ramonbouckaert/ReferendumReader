package io.bouckaert.referendumreader

sealed class ElectorateResults(
    val results: Results
) {
    class Detailed(
        val electorate: Electorate,
        results: Results,
        val totalEnrolled: Long
    ) : ElectorateResults(results)

    class Light(
        val electorateId: Long,
        results: Results
    ) : ElectorateResults(results)

    data class Electorate(
        val state: String,
        val electorateId: String,
        val electorateShortCode: String,
        val electorateName: String,
    )

    data class Results(
        val boothsReturned: Long,
        val boothsTotal: Long,
        val yes: Long,
        val no: Long,
        val informal: Long
    )

    override fun toString(): String {
        val b = StringBuilder()
        when (this) {
            is Detailed -> b.append("${electorate.state} - ${electorate.electorateName}")
            is Light -> b.append(electorateId)
        }
        b.append(" - ")
        b.append("Yes: ${results.yes}")
        b.append(", ")
        b.append("No: ${results.no}")
        b.append(", ")
        b.append("Informal: ${results.informal}")
        b.append(", ")
        b.append("Booths Returned: ${(results.boothsReturned.toFloat() / results.boothsTotal.toFloat()) * 100}%")
        if (this is Detailed) {
            b.append(", ")
            b.append("Total enrolled: $totalEnrolled")
        }
        return b.toString()
    }
}
