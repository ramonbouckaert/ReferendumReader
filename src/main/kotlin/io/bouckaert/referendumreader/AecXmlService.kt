package io.bouckaert.referendumreader

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader

class AecXmlService(
    private val ftpService: AecFtpService
) {
    private val xmlInputFactory = XMLInputFactory.newFactory()
    fun getPreloadData(): Flow<ElectorateResults.Detailed> = flow {
        ftpService.preloadStream.use { inputStream ->
            val xr = xmlInputFactory.createXMLStreamReader(inputStream.delegate)

            // Skip to Polling Districts
            while (xr.hasNext()) {
                xr.next()
                if (xr.eventType == XMLStreamConstants.START_ELEMENT && xr.localName == "PollingDistricts") break
            }

            while (xr.hasNext()) {
                xr.next()
                if (xr.eventType == XMLStreamConstants.END_ELEMENT && xr.localName == "PollingDistricts") break
                if (xr.eventType == XMLStreamConstants.START_ELEMENT && xr.localName == "PollingDistrict") {
                    val results = readPreloadPollingDistrict(xr)
                    if (results != null) emit(results)
                }
            }
        }
    }

    fun getLightProgressData(): Flow<ElectorateResults.Light> = flow {
        ftpService.lightProgressStream.use { inputStream ->
            val xr = xmlInputFactory.createXMLStreamReader(inputStream.delegate)

            // Skip to Polling Districts
            while (xr.hasNext()) {
                xr.next()
                if (xr.eventType == XMLStreamConstants.START_ELEMENT && xr.localName == "PollingDistricts") break
            }

            while (xr.hasNext()) {
                xr.next()
                if (xr.eventType == XMLStreamConstants.END_ELEMENT && xr.localName == "PollingDistricts") break
                if (xr.eventType == XMLStreamConstants.START_ELEMENT && xr.localName == "PollingDistrict") {
                    val results = readLightProgressPollingDistrict(xr)
                    if (results != null) emit(results)
                }
            }
        }
    }

    private fun readPreloadPollingDistrict(xr: XMLStreamReader): ElectorateResults.Detailed? {
        var electorate: ElectorateResults.Electorate? = null
        var results: ElectorateResults.Results? = null
        var totalEnrolled: Long? = null
        while (xr.hasNext()) {
            xr.next()
            if (xr.eventType == XMLStreamConstants.END_ELEMENT && xr.localName == "PollingDistrict") break
            if (xr.eventType == XMLStreamConstants.START_ELEMENT) {
                when (xr.localName) {
                    "PollingDistrictIdentifier" -> electorate = readPreloadPollingDistrictIdentifier(xr)
                    "Enrolment" -> totalEnrolled = readEnrolment(xr)
                    "ProposalResults" -> results = readProposalResults(xr)
                }
            }
        }
        return when {
            electorate == null || results == null || totalEnrolled == null -> null
            else ->
                ElectorateResults.Detailed(
                    electorate,
                    results,
                    totalEnrolled
                )
        }
    }

    private fun readLightProgressPollingDistrict(xr: XMLStreamReader): ElectorateResults.Light? {
        var electorateId: Long? = null
        var results: ElectorateResults.Results? = null
        while (xr.hasNext()) {
            xr.next()
            if (xr.eventType == XMLStreamConstants.END_ELEMENT && xr.localName == "PollingDistrict") break
            if (xr.eventType == XMLStreamConstants.START_ELEMENT) {
                when (xr.localName) {
                    "PollingDistrictIdentifier" -> electorateId = readLightProgressPollingDistrictIdentifier(xr)
                    "ProposalResults" -> results = readProposalResults(xr)
                }
            }
        }
        return when {
            electorateId == null || results == null -> null
            else ->
                ElectorateResults.Light(
                    electorateId,
                    results
                )
        }
    }

    private fun readPreloadPollingDistrictIdentifier(xr: XMLStreamReader): ElectorateResults.Electorate? {
        var electorateId: String? = null
        var electorateShortCode: String? = null
        var electorateName: String? = null
        var state: String? = null

        for (i in 0..<xr.attributeCount) {
            when (xr.getAttributeLocalName(i)) {
                "Id" -> electorateId = xr.getAttributeValue(i)
                "ShortCode" -> electorateShortCode = xr.getAttributeValue(i)
            }
        }
        while (xr.hasNext()) {
            xr.next()
            if (xr.eventType == XMLStreamConstants.END_ELEMENT && xr.localName == "PollingDistrictIdentifier") break
            if (xr.eventType == XMLStreamConstants.START_ELEMENT) {
                when (xr.localName) {
                    "Name" -> electorateName = readCharacters(xr)
                    "StateIdentifier" -> {
                        for (i in 0..<xr.attributeCount) {
                            when (xr.getAttributeLocalName(i)) {
                                "Id" -> state = xr.getAttributeValue(i)
                            }
                        }
                    }
                }
            }
        }

        return when {
            state == null || electorateId == null || electorateShortCode == null || electorateName == null -> null
            else -> ElectorateResults.Electorate(state, electorateId, electorateShortCode, electorateName)
        }
    }

    private fun readLightProgressPollingDistrictIdentifier(xr: XMLStreamReader): Long? {
        for (i in 0..<xr.attributeCount) {
            when (xr.getAttributeLocalName(i)) {
                "Id" -> return xr.getAttributeValue(i).toLongOrNull()
            }
        }
        return null
    }

    private fun readCharacters(xr: XMLStreamReader): String {
        val result = StringBuilder()
        while (xr.hasNext()) {
            when (xr.next()) {
                XMLStreamReader.CHARACTERS, XMLStreamReader.CDATA -> {
                    result.append(xr.text)
                }
                XMLStreamReader.END_ELEMENT -> return result.toString()
            }
        }
        throw XMLStreamException("Premature end of file")
    }

    private fun readEnrolment(xr: XMLStreamReader): Long? {
        for (i in 0..<xr.attributeCount) {
            when (xr.getAttributeLocalName(i)) {
                "CloseOfRolls" -> return xr.getAttributeValue(i).toLongOrNull()
            }
        }
        return readCharacters(xr).toLongOrNull()
    }

    private fun readProposalResults(xr: XMLStreamReader): ElectorateResults.Results? {
        var boothsReturned: Long? = null
        var boothsTotal: Long? = null
        var yes: Long? = null
        var no: Long? = null
        var informal: Long? = null

        for (i in 0..<xr.attributeCount) {
            when (xr.getAttributeLocalName(i)) {
                "PollingPlacesReturned" -> boothsReturned = xr.getAttributeValue(i).toLongOrNull()
                "PollingPlacesExpected" -> boothsTotal = xr.getAttributeValue(i).toLongOrNull()
            }
        }
        while (xr.hasNext()) {
            xr.next()
            if (xr.eventType == XMLStreamConstants.END_ELEMENT && xr.localName == "ProposalResults") break
            if (xr.eventType == XMLStreamConstants.START_ELEMENT) {
                when (xr.localName) {
                    "Option" -> {
                        val option = readOption(xr)
                        if (option != null) {
                            when (option.first) {
                                "Y" -> yes = option.second
                                "N" -> no = option.second
                            }
                        }
                    }
                    "Informal" -> informal = readInformal(xr)
                }
            }
        }

        return when {
            boothsReturned == null || boothsTotal == null || yes == null || no == null || informal == null -> null
            else -> ElectorateResults.Results(
                boothsReturned,
                boothsTotal,
                yes,
                no,
                informal
            )
        }
    }

    private fun readOption(xr: XMLStreamReader): Pair<String, Long>? {
        var option: String? = null
        var count: Long? = null
        while (xr.hasNext()) {
            xr.next()
            if (xr.eventType == XMLStreamConstants.END_ELEMENT && xr.localName == "Option") break
            if (xr.eventType == XMLStreamConstants.START_ELEMENT) {
                when (xr.localName) {
                    "Votes" -> count = readCharacters(xr).toLongOrNull()
                    "ReferendumOptionIdentifier" -> {
                        for (i in 0..<xr.attributeCount) {
                            when (xr.getAttributeLocalName(i)) {
                                "Id" -> option = xr.getAttributeValue(i)
                            }
                        }
                    }
                }
            }
        }
        return when {
            option == null || count == null -> null
            else -> option to count
        }
    }

    private fun readInformal(xr: XMLStreamReader): Long? {
        var informal: Long? = null
        while (xr.hasNext()) {
            xr.next()
            if (xr.eventType == XMLStreamConstants.END_ELEMENT && xr.localName == "Informal") break
            if (xr.eventType == XMLStreamConstants.START_ELEMENT && xr.localName == "Votes") {
                informal = readCharacters(xr).toLongOrNull()
            }
        }
        return informal
    }
}