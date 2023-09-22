package io.bouckaert.referendumreader.model

data class Contest(
    val enrolment: Long,
    val proposalResults: ProposalResults,
    val pollingDistricts: List<PollingDistrict>
)
