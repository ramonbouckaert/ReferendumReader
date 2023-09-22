package io.bouckaert.referendumreader

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import io.bouckaert.referendumreader.model.Contest
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

        AecFtpService(config.aec.host, config.aec.port, config.aec.electoralEvent).use { ftpService ->
            val xmlService = AecXmlService(ftpService)
            runBlocking {
                while (true) {
                    delay(config.pollingFrequency.toDuration(DurationUnit.SECONDS))
                    val data = xmlService.getVerboseData()
                    printContest(data)
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