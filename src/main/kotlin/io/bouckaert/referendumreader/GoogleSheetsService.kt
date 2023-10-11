package io.bouckaert.referendumreader

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import io.bouckaert.referendumreader.output.BoothResults
import java.io.*


class GoogleSheetsService {
    companion object {
        private const val APPLICATION_NAME = "Google Sheets API ReferendumReader"
    }

    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    private val scopes = listOf(SheetsScopes.SPREADSHEETS)
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val spreadsheetId = "1--kC9YH5Q8igqvnLuHTnUZfZl7HU06MG6bgKVNPqVlU"
    private val service = Sheets.Builder(httpTransport, jsonFactory, getCredentials(httpTransport)).setApplicationName(APPLICATION_NAME).build()

    private fun getCredentials(httpTransport: NetHttpTransport): Credential {
        // Load client secrets.
        val inputStream: InputStream = ByteArrayInputStream(
            """
                {"installed":{"client_id":"976726326152-62ut7sdsur3qaqvu2mf1gl85v0pel7ti.apps.googleusercontent.com","project_id":"referendumreader","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_secret":"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","redirect_uris":["http://localhost"]}}
            """.trimIndent().toByteArray()
        )
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(inputStream))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes
        )
            .setDataStoreFactory(MemoryDataStoreFactory())
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    fun uploadResults(results: List<BoothResults>) {
        val updateRequest = BatchUpdateSpreadsheetRequest()

        updateRequest.requests = listOf(
            Request().setUpdateCells(
                UpdateCellsRequest().apply {
                    fields = "userEnteredValue"
                    start = GridCoordinate().apply {
                        rowIndex = 0
                        columnIndex = 0
                    }
                    rows = listOf(
                        RowData().setValues(
                            listOf(
                                CellData().apply { userEnteredValue = ExtendedValue().setStringValue("id") },
                                CellData().apply { userEnteredValue = ExtendedValue().setStringValue("name") },
                                CellData().apply { userEnteredValue = ExtendedValue().setStringValue("yes") },
                                CellData().apply { userEnteredValue = ExtendedValue().setStringValue("no") },
                                CellData().apply { userEnteredValue = ExtendedValue().setStringValue("informal") },
                                CellData().apply { userEnteredValue = ExtendedValue().setStringValue("total") },
                            )
                        )
                    ).plus(
                        results.map {
                            RowData().setValues(
                                listOf(
                                    CellData().apply { userEnteredValue = ExtendedValue().setNumberValue(it.id.toDouble()) },
                                    CellData().apply { userEnteredValue = ExtendedValue().setStringValue(it.name) },
                                    CellData().apply { userEnteredValue = ExtendedValue().setNumberValue(it.yes.toDouble()) },
                                    CellData().apply { userEnteredValue = ExtendedValue().setNumberValue(it.no.toDouble()) },
                                    CellData().apply { userEnteredValue = ExtendedValue().setNumberValue(it.informal.toDouble()) },
                                    CellData().apply { userEnteredValue = ExtendedValue().setNumberValue(it.total.toDouble()) },
                                )
                            )
                        }
                    )
                }
            )
        )

        service.spreadsheets().batchUpdate(spreadsheetId, updateRequest).execute()
    }
}