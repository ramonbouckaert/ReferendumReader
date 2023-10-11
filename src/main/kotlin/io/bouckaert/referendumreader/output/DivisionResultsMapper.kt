package io.bouckaert.referendumreader.output

import io.bouckaert.referendumreader.model.Contest

object DivisionResultsMapper {
    @JvmStatic
    fun Contest.map(): List<DivisionResults> =
        this.pollingDistricts
            .map {
                DivisionResults(
                    it.pollingDistrictIdentifier.id.toInt(),
                    it.pollingDistrictIdentifier.name,
                    it.pollingDistrictIdentifier.stateIdentifier.name,
                    it.pollingDistrictIdentifier.shortCode,
                    it.proposalResults.options.first { opt -> opt.referendumOptionIdentifier.id == "Y" }.votes,
                    it.proposalResults.options.first { opt -> opt.referendumOptionIdentifier.id == "N" }.votes,
                    it.proposalResults.informal.votes,
                    it.proposalResults.total.votes
                )
            }
}