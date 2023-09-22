package io.bouckaert.referendumreader

import io.bouckaert.referendumreader.model.Contest
import io.bouckaert.referendumreader.model.PollingDistrict
import io.bouckaert.referendumreader.model.PollingPlace
import io.bouckaert.referendumreader.model.ProposalResults
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants.*
import javax.xml.stream.XMLStreamReader

class AecXmlService(
    private val ftpService: AecFtpService
) {
    private val xmlInputFactory = XMLInputFactory.newFactory()
    private val referendumOptionIdentifierCache: MutableMap<String, ProposalResults.Option.ReferendumOptionIdentifier> = mutableMapOf()
    private val pollingDistrictIdentifierCache: MutableMap<String, PollingDistrict.PollingDistrictIdentifier> = mutableMapOf()
    private val pollingPlaceIdentifierCache: MutableMap<String, PollingPlace.PollingPlaceIdentifier> = mutableMapOf()

    // Creating Contest
    fun getVerboseData(): Contest {
        ftpService.verboseStream.use { inputStream ->
            val xr = xmlInputFactory.createXMLStreamReader(inputStream.delegate)

            var enrolment: Long? = null
            var proposalResults: ProposalResults? = null
            var pollingDistricts: List<PollingDistrict>? = null

            xr.forEachWithinElement("Contest") {
                if (eventType == START_ELEMENT) {
                    when (localName) {
                        "Enrolment" -> enrolment = readEnrolment()
                        "ProposalResults" -> proposalResults = readProposalResults()
                        "PollingDistricts" -> pollingDistricts = readPollingDistricts()
                    }
                }
            }

            return when {
                enrolment == null -> throw RuntimeException("Contest.enrolment could not be found")
                proposalResults == null -> throw RuntimeException("Contest.proposalResults could not be found")
                pollingDistricts == null -> throw RuntimeException("Contest.pollingDistricts could not be found")
                else -> Contest(enrolment!!, proposalResults!!, pollingDistricts!!)
            }
        }
    }
    private fun XMLStreamReader.readEnrolment(): Long {
        skipToStartElement("Enrolment")
        for (i in 0..<attributeCount) {
            when (getAttributeLocalName(i)) {
                "CloseOfRolls" -> return getAttributeValue(i).toLong()
            }
        }
        return readTagContentAsString().toLong()
    }

    private fun XMLStreamReader.readProposalResults(): ProposalResults {
        val options: MutableList<ProposalResults.Option> = mutableListOf()
        var formal: ProposalResults.Count? = null
        var informal: ProposalResults.Count? = null
        var total: ProposalResults.Count? = null

        forEachWithinElement("ProposalResults") {
            if (eventType == START_ELEMENT) {
                when (localName) {
                    "Option" -> options.add(readOption())
                    "Formal" -> formal = readCount("Formal")
                    "Informal" -> informal = readCount("Informal")
                    "Total" -> total = readCount("Total")
                }
            }
        }

        return when {
            options.isEmpty() -> throw RuntimeException("ProposalResults has no options")
            formal == null -> throw RuntimeException("ProposalResults.formal could not be found")
            informal == null -> throw RuntimeException("ProposalResults.informal could not be found")
            total == null -> throw RuntimeException("ProposalResults.total could not be found")
            else -> ProposalResults(options, formal!!, informal!!, total!!)
        }
    }

    private fun XMLStreamReader.readOption(): ProposalResults.Option {
        var referendumOptionIdentifier: ProposalResults.Option.ReferendumOptionIdentifier? = null
        var votes: Long? = null

        forEachWithinElement("Option") {
            if (eventType == START_ELEMENT) {
                when (localName) {
                    "ReferendumOptionIdentifier" -> referendumOptionIdentifier = readReferendumOptionIdentifier()
                    "Votes" -> votes = readTagContentAsString().toLong()
                }
            }
        }

        when {
            referendumOptionIdentifier == null -> throw RuntimeException("Option.referendumOptionIdentifier could not be found")
            votes == null -> throw RuntimeException("Option.votes could not be found")
            else -> return ProposalResults.Option(referendumOptionIdentifier!!, votes!!)
        }
    }

    private fun XMLStreamReader.readCount(elementName: String): ProposalResults.Count {
        var votes: Long? = null
        forEachWithinElement(elementName) {
            if (eventType == START_ELEMENT) {
                when (localName) {
                    "Votes" -> votes = readTagContentAsString().toLong()
                }
            }
        }
        when {
            votes == null -> throw RuntimeException("$elementName.votes could not be found")
            else -> return ProposalResults.Count(votes!!)
        }
    }

    private fun XMLStreamReader.readReferendumOptionIdentifier(): ProposalResults.Option.ReferendumOptionIdentifier {
        var id: String? = null
        val name: String?
        skipToStartElement("ReferendumOptionIdentifier")
        for (i in 0..<attributeCount) {
            when (getAttributeLocalName(i)) {
                "Id" -> {
                    id = getAttributeValue(i)
                    if (referendumOptionIdentifierCache.containsKey(id)) return referendumOptionIdentifierCache[id]!!
                }
            }
        }
        name = readTagContentAsString()
        return when {
            id == null -> throw RuntimeException("ReferendumOptionIdentifier.id could not be found")
            else -> ProposalResults.Option.ReferendumOptionIdentifier(id, name)
        }.apply { referendumOptionIdentifierCache[id] = this }
    }

    private fun XMLStreamReader.readPollingDistricts(): List<PollingDistrict> {
        val pollingDistricts: MutableList<PollingDistrict> = mutableListOf()
        forEachWithinElement("PollingDistricts") {
            if (eventType == START_ELEMENT) {
                when (localName) {
                    "PollingDistrict" -> pollingDistricts.add(readPollingDistrict())
                }
            }
        }
        return pollingDistricts
    }

    private fun XMLStreamReader.readPollingDistrict(): PollingDistrict {
        var pollingDistrictIdentifier: PollingDistrict.PollingDistrictIdentifier? = null
        var enrolment: Long? = null
        var proposalResults: ProposalResults? = null
        var pollingPlaces: List<PollingPlace>? = null

        forEachWithinElement("PollingDistrict") {
            if (eventType == START_ELEMENT) {
                when (localName) {
                    "PollingDistrictIdentifier" -> pollingDistrictIdentifier = readPollingDistrictIdentifier()
                    "Enrolment" -> enrolment = readEnrolment()
                    "ProposalResults" -> proposalResults = readProposalResults()
                    "PollingPlaces" -> pollingPlaces = readPollingPlaces()
                }
            }
        }

        return when {
            pollingDistrictIdentifier == null ->throw RuntimeException("PollingDistrict.pollingDistrictIdentifier could not be found")
            enrolment == null ->throw RuntimeException("PollingDistrict.enrolment could not be found")
            proposalResults == null ->throw RuntimeException("PollingDistrict.proposalResults could not be found")
            pollingPlaces == null ->throw RuntimeException("PollingDistrict.pollingPlaces could not be found")
            else -> PollingDistrict(pollingDistrictIdentifier!!, enrolment!!, proposalResults!!, pollingPlaces!!)
        }
    }

    private fun XMLStreamReader.readPollingDistrictIdentifier(): PollingDistrict.PollingDistrictIdentifier {
        var id: String? = null
        var shortCode: String? = null
        var name: String? = null
        var stateIdentifier: PollingDistrict.PollingDistrictIdentifier.StateIdentifier? = null

        skipToStartElement("PollingDistrictIdentifier")
        for (i in 0..<attributeCount) {
            when (getAttributeLocalName(i)) {
                "Id" -> {
                    id = getAttributeValue(i)
                    if (pollingDistrictIdentifierCache.containsKey(id)) return pollingDistrictIdentifierCache[id]!!
                }
                "ShortCode" -> shortCode = getAttributeValue(i)
            }
        }
        forEachWithinElement("PollingDistrictIdentifier") {
            if (eventType == START_ELEMENT) {
                when (localName) {
                    "Name" -> name = readTagContentAsString()
                    "StateIdentifier" -> stateIdentifier = readStateIdentifier()
                }
            }
        }

        return when {
            id == null -> throw RuntimeException("PollingDistrictIdentifier.id could not be found")
            shortCode == null -> throw RuntimeException("PollingDistrictIdentifier.shortCode could not be found")
            name == null -> throw RuntimeException("PollingDistrictIdentifier.name could not be found")
            stateIdentifier == null -> throw RuntimeException("PollingDistrictIdentifier.stateIdentifier could not be found")
            else -> PollingDistrict.PollingDistrictIdentifier(id, shortCode, name!!, stateIdentifier!!)
        }.apply { pollingDistrictIdentifierCache[id] = this }
    }

    private fun XMLStreamReader.readStateIdentifier(): PollingDistrict.PollingDistrictIdentifier.StateIdentifier {
        skipToStartElement("StateIdentifier")
        for (i in 0..<attributeCount) {
            when (getAttributeLocalName(i)) {
                "Id" -> return getAttributeValue(i).let { PollingDistrict.PollingDistrictIdentifier.StateIdentifier.valueOf(it) }
            }
        }
        throw RuntimeException("StateIdentifier.id could not be found")
    }

    private fun XMLStreamReader.readPollingPlaces(): List<PollingPlace> {
        val pollingPlaces: MutableList<PollingPlace> = mutableListOf()
        forEachWithinElement("PollingPlaces") {
            if (eventType == START_ELEMENT) {
                when (localName) {
                    "PollingPlace" -> pollingPlaces.add(readPollingPlace())
                }
            }
        }
        return pollingPlaces
    }

    private fun XMLStreamReader.readPollingPlace(): PollingPlace {
        var pollingPlaceIdentifier: PollingPlace.PollingPlaceIdentifier? = null
        var proposalResults: ProposalResults? = null

        forEachWithinElement("PollingPlace") {
            if (eventType == START_ELEMENT) {
                when (localName) {
                    "PollingPlaceIdentifier" -> pollingPlaceIdentifier = readPollingPlaceIdentifier()
                    "ProposalResults" -> proposalResults = readProposalResults()
                }
            }
        }

        return when {
            pollingPlaceIdentifier == null -> throw RuntimeException("PollingPlace.pollingPlaceIdentifier could not be found")
            proposalResults == null -> throw RuntimeException("PollingPlace.proposalResults could not be found")
            else -> PollingPlace(pollingPlaceIdentifier!!, proposalResults!!)
        }
    }

    private fun XMLStreamReader.readPollingPlaceIdentifier(): PollingPlace.PollingPlaceIdentifier {
        var id: String? = null
        var name: String? = null
        var classification: String? = null

        skipToStartElement("PollingPlaceIdentifier")
        for (i in 0..<attributeCount) {
            when (getAttributeLocalName(i)) {
                "Id" -> {
                    id = getAttributeValue(i)
                    if (pollingPlaceIdentifierCache.containsKey(id)) return pollingPlaceIdentifierCache[id]!!
                }
                "Name" -> name = getAttributeValue(i)
                "Classification" -> classification = getAttributeValue(i)
            }
        }

        return when {
            id == null -> throw RuntimeException("PollingPlaceIdentifier.id could not be found")
            name == null -> throw RuntimeException("PollingPlaceIdentifier.name could not be found")
            else -> PollingPlace.PollingPlaceIdentifier(id, name, classification)
        }.apply { pollingPlaceIdentifierCache[id] = this }
    }

    // Updating Contest
//
//    fun updateVerboseData(contest: Contest): Contest {
//        var success = true
//        ftpService.verboseStream.use { inputStream ->
//            val xr = xmlInputFactory.createXMLStreamReader(inputStream.delegate)
//
//            xr.forEachWithinElement("Contest") {
//                if (eventType == START_ELEMENT) {
//                    when (localName) {
//                        "ProposalResults" -> success = success && updateProposalResults(contest.proposalResults)
//                        "PollingDistricts" -> success = success && updatePollingDistricts(contest.pollingDistricts)
//                    }
//                }
//            }
//        }
//        return if (!success) {
//            println("Warning: Failed to update existing in-memory Contest, reconstructing instead")
//            getVerboseData()
//        } else contest
//    }
//
//    private fun XMLStreamReader.updateProposalResults(proposalResults: ProposalResults): Boolean {
//        forEachWithinElement("ProposalResults") {
//            if (eventType == START_ELEMENT) {
//                when (localName) {
//                    "Option" -> updateOption(proposalResults.options) || return false
//                    "Formal" -> updateCount(proposalResults.formal, "Formal") || return false
//                    "Informal" -> updateCount(proposalResults.informal, "Informal") || return false
//                    "Total" -> updateCount(proposalResults.total, "Total") || return false
//                }
//            }
//        }
//        return true
//    }
//
//    private fun XMLStreamReader.updatePollingDistricts(pollingDistricts: Collection<PollingDistrict>): Boolean {
//        forEachWithinElement("PollingDistricts") {
//            if (eventType == START_ELEMENT) {
//                when (localName) {
//                    "PollingDistrict" -> updatePollingDistrict(pollingDistricts) || return false
//                }
//            }
//        }
//        return true
//    }
//
//    private fun XMLStreamReader.updatePollingDistrict(pollingDistricts: Collection<PollingDistrict>): Boolean {
//        var pollingDistrictIdentifier: PollingDistrict.PollingDistrictIdentifier? = null
//
//        forEachWithinElement("PollingDistrict") {
//            if (eventType == START_ELEMENT) {
//                when (localName) {
//                    "PollingDistrictIdentifier" -> pollingDistrictIdentifier = readPollingDistrictIdentifier()
//                    "ProposalResults" -> {
//                        if (pollingDistrictIdentifier == null) return false
//                        val pd = pollingDistricts.find { it.pollingDistrictIdentifier == pollingDistrictIdentifier } ?: return false
//                        updateProposalResults(pd.proposalResults) || return false
//                    }
//                    "PollingPlaces" -> {
//                        if (pollingDistrictIdentifier == null) return false
//                        val pd = pollingDistricts.find { it.pollingDistrictIdentifier == pollingDistrictIdentifier } ?: return false
//                        updatePollingPlaces(pd.pollingPlaces) || return false
//                    }
//                }
//            }
//        }
//
//        return true
//    }
//
//    private fun XMLStreamReader.updatePollingPlaces(pollingPlaces: Collection<PollingPlace>): Boolean {
//        forEachWithinElement("PollingPlaces") {
//            if (eventType == START_ELEMENT) {
//                when (localName) {
//                    "PollingPlace" -> updatePollingPlace(pollingPlaces) || return false
//                }
//            }
//        }
//        return true
//    }
//
//    private fun XMLStreamReader.updatePollingPlace(pollingPlaces: Collection<PollingPlace>): Boolean {
//        var pollingPlaceIdentifier: PollingPlace.PollingPlaceIdentifier? = null
//
//        forEachWithinElement("PollingPlace") {
//            if (eventType == START_ELEMENT) {
//                when (localName) {
//                    "PollingPlaceIdentifier" -> pollingPlaceIdentifier = readPollingPlaceIdentifier()
//                    "ProposalResults" -> {
//                        if (pollingPlaceIdentifier == null) return false
//                        val pp = pollingPlaces.find { it.pollingPlaceIdentifier == pollingPlaceIdentifier } ?: return false
//                        updateProposalResults(pp.proposalResults) || return false
//                    }
//                }
//            }
//        }
//
//        return true
//    }
//
//    private fun XMLStreamReader.updateOption(options: Collection<ProposalResults.Option>): Boolean {
//        var referendumOptionIdentifier: ProposalResults.Option.ReferendumOptionIdentifier? = null
//        var votes: Long? = null
//
//        forEachWithinElement("Option") {
//            if (eventType == START_ELEMENT) {
//                when (localName) {
//                    "ReferendumOptionIdentifier" -> referendumOptionIdentifier = readReferendumOptionIdentifier()
//                    "Votes" -> votes = readTagContentAsString().toLong()
//                }
//            }
//        }
//
//        if (referendumOptionIdentifier == null) return false
//        if (votes == null) return false
//
//        val option = options.find { opt -> opt.referendumOptionIdentifier.id == referendumOptionIdentifier!!.id } ?: return false
//        option.votes = votes!!
//        return true
//    }
//
//    private fun XMLStreamReader.updateCount(count: ProposalResults.Count, elementName: String): Boolean {
//        forEachWithinElement(elementName) {
//            if (eventType == START_ELEMENT) {
//                when (localName) {
//                    "Votes" -> {
//                        count.votes = readTagContentAsString().toLong()
//                        return true
//                    }
//                }
//            }
//        }
//        return false
//    }

    // Common

    private inline fun XMLStreamReader.forEachWithinElement(elementName: String, block: XMLStreamReader.() -> Unit) {
        skipToStartElement(elementName)
        while (hasNext()) {
            next()
            if (eventType == END_ELEMENT && localName == elementName) return
            block()
        }
        throw RuntimeException("XML Parse Error: Unexpected EOF, did not find closing bracket for element $elementName")
    }

    private fun XMLStreamReader.skipToStartElement(elementName: String) {
        if (eventType == START_ELEMENT && localName == elementName) return
        while (hasNext()) {
            next()
            if (eventType == START_ELEMENT && localName == elementName) return
        }
        throw RuntimeException("XML Parse Error: Unexpected EOF, did not find opening bracket for element $elementName")
    }

    private fun XMLStreamReader.readTagContentAsString(): String {
        val result = StringBuilder()
        while (hasNext()) {
            when (next()) {
                XMLStreamReader.CHARACTERS, XMLStreamReader.CDATA -> {
                    result.append(text)
                }
                XMLStreamReader.END_ELEMENT -> return result.toString()
            }
        }
        throw RuntimeException("Unexpected EOF: EOF Reached while reading tag content as a string")
    }
}