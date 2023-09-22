package io.bouckaert.referendumreader.model

data class PollingDistrict(
    val pollingDistrictIdentifier: PollingDistrictIdentifier,
    val enrolment: Long,
    val proposalResults: ProposalResults,
    val pollingPlaces: List<PollingPlace>
) {
    data class PollingDistrictIdentifier(
        val id: String,
        val shortCode: String,
        val name: String,
        val stateIdentifier: StateIdentifier
    ) {
        enum class StateIdentifier { ACT, NT, NSW, VIC, SA, QLD, TAS, WA }
    }
}