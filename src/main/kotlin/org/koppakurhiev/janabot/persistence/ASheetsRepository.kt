package org.koppakurhiev.janabot.persistence

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import org.koppakurhiev.janabot.IBot
import org.koppakurhiev.janabot.common.getLogger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.security.GeneralSecurityException

abstract class ASheetsRepository<T>(private val bot: IBot) : IRepository<T> {
    private val tokensDirectory = bot.properties.getProperty("sheets.tokens")
    private val credentialsPath = bot.properties.getProperty("sheets.credentials")
    private val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()
    private val scope = listOf(SheetsScopes.SPREADSHEETS)

    abstract fun parse(values: List<List<Any>>): T?

    protected fun save(sheetsId: String, updateCoordinates: String, values: List<List<String>>): Boolean {
        val service = buildSheetsService()
        val range = ValueRange()
        range.setValues(values)
        val request: Sheets.Spreadsheets.Values.Update =
            service.spreadsheets().values().update(sheetsId, updateCoordinates, range)
        request.valueInputOption = "RAW"
        val response = request.execute()
        getLogger().trace { response }
        return response.updatedCells > 0
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    protected fun load(sheetsId: String, sheetName: String): T? {
        val service = buildSheetsService()
        val response: ValueRange = service.spreadsheets().values()[sheetsId, sheetName]
            .execute()
        val values: List<List<Any>> = response.getValues()
        return if (values.isEmpty()) {
            null
        } else {
            parse(values)
        }
    }

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    @Throws(IOException::class)
    private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential {
        // Load client secrets.
        val credentials = ASheetsRepository::class.java.getResourceAsStream(credentialsPath)
            ?: throw FileNotFoundException("Resource not found: $credentialsPath")
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(credentials))
        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, jsonFactory, clientSecrets, scope)
            .setDataStoreFactory(FileDataStoreFactory(File(tokensDirectory)))
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("telegram_bot")
    }

    private fun buildSheetsService(): Sheets {
        // Build a new authorized API client service.
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        return Sheets.Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
            .setApplicationName(bot.properties.getProperty("bot.username"))
            .build()
    }
}