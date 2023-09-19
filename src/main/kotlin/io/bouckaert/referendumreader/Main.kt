package io.bouckaert.referendumreader

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = ConfigLoaderBuilder.default().apply {
            addFileSource(args.firstOrNull()?.ifBlank { "config.json" } ?: "config.json")
        }.build().loadConfigOrThrow<AppConfig>()

        AecFtpService(config.aec.host, config.aec.port, config.aec.electoralEvent).use { ftpService ->
            val xmlService = AecXmlService(ftpService)
            runBlocking {
                xmlService.getPreloadData().collect {
                    println(it)
                }
                while (true) {
                    delay(1000)
                    xmlService.getLightProgressData().collect {
                        println(it)
                    }
                }
            }
        }
    }
}