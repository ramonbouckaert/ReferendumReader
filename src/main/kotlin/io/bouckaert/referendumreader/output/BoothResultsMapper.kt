package io.bouckaert.referendumreader.output

import io.bouckaert.referendumreader.model.Contest

object BoothResultsMapper {
    @JvmStatic
    fun Contest.map(): List<BoothResults> =
        this.pollingDistricts
            .flatMap { it.pollingPlaces }
            .map {
                BoothResults(
                    it.pollingPlaceIdentifier.id.toInt(),
                    it.pollingPlaceIdentifier.name,
                    it.proposalResults.options.first { opt -> opt.referendumOptionIdentifier.id == "Y" }.votes,
                    it.proposalResults.options.first { opt -> opt.referendumOptionIdentifier.id == "N" }.votes,
                    it.proposalResults.informal.votes,
                    it.proposalResults.total.votes
                )
            }
}