package io.bouckaert.referendumreader

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import io.bouckaert.referendumreader.model.Contest
import io.bouckaert.referendumreader.output.BoothResultsMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = ConfigLoaderBuilder.default().apply {
            addFileSource(args.firstOrNull()?.ifBlank { "config.json" } ?: "config.json")
        }.build().loadConfigOrThrow<AppConfig>()

        val googleSheetsService = GoogleSheetsService()

        AecFtpService(config.aec.host, config.aec.port, config.aec.electoralEvent).use { ftpService ->
            val xmlService = AecXmlService(ftpService)
            runBlocking {
                while (true) {
                    delay(config.pollingFrequency.toDuration(DurationUnit.SECONDS))
                    val data = xmlService.getVerboseData()
                    var boothsReporting = 0
                    var boothsTotal = 0
                    val totalVotes = data.pollingDistricts.fold(0L) { acc, district ->
                        acc + district.pollingPlaces.fold(0L) { acc2, pollingPlace ->
                            boothsTotal++
                            if (pollingPlace.proposalResults.total.votes > 0) boothsReporting++
                            acc2 + pollingPlace.proposalResults.total.votes
                        }
                    }
                    println("Updating spreadsheet. Booths reporting: $boothsReporting/$boothsTotal Total votes counted: $totalVotes")
                    with (BoothResultsMapper) {
                        googleSheetsService.uploadResults(data.map())
                    }
                }
            }
        }
    }

    private fun printContest(data: Contest) {
        println("Total count:")
        println(data.proposalResults)
        println("By division count:")
        data.pollingDistricts.forEach {
            println("${it.pollingDistrictIdentifier.stateIdentifier} - ${it.pollingDistrictIdentifier.name} - ${it.proposalResults}")
        }
        println("By booth count:")
        data.pollingDistricts.forEach { div ->
            div.pollingPlaces.forEach {
                println("${it.pollingPlaceIdentifier.name} - ${it.proposalResults}")
            }
        }
    }
}