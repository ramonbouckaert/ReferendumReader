package io.bouckaert.referendumreader.model

data class PollingPlace(
    val pollingPlaceIdentifier: PollingPlaceIdentifier,
    val proposalResults: ProposalResults
) {
    data class PollingPlaceIdentifier(
        val id: String,
        val name: String,
        val classification: String?
    )
}