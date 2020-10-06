package com.chutneytesting.idea.util

import com.chutneytesting.idea.logger.EventDataLogger
import com.chutneytesting.idea.settings.ChutneySettings
import com.google.gson.Gson
import com.intellij.notification.NotificationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import com.intellij.util.net.ssl.CertificateManager
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection


object ChutneyServerApiUtils {

    val LOG = Logger.getInstance(HttpRequests::class.java)

    fun checkRemoteServerUrlConfig(project: Project): Boolean {
        val settingsInstance = ChutneySettings.getInstance()
        if (getRemoteServerUrl().isNullOrBlank() || settingsInstance.getRemoteUser().isNullOrBlank() || settingsInstance.getRemotePassword().isNullOrBlank()) {
            EventDataLogger.logError(
                    " <a href=\"configure\">Configure</a> Missing remote configuration server, please check url, user and password",
                    project,
                    NotificationListener { notification, hyperlinkEvent ->
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Chutney")
                    })
            return false
        }
        return true
    }

    fun getRemoteServerUrl(): String? {
        return try {
            val url = URL(ChutneySettings.getInstance().getRemoteServerUrl())
            url.protocol + "://" + url.host + if (url.port == -1) "" else (":" + url.port)
        } catch (e: Exception) {
            null
        }
    }

    fun getRemoteDatabaseUrl() = "${ChutneyServerApiUtils.getRemoteServerUrl()}/api/v1/admin/database/execute/jdbc"


    inline fun <reified T> post(query: String, body: String): T {
        return execute<T>(query, "POST", body)
    }

    inline fun <reified T> get(query: String): T {
        return execute<T>(query, "GET", "")
    }

    inline fun <reified T> execute(query: String, requestMethod: String, body: String): T {
        val url = URL(query)

        val remoteUser = ChutneySettings.getInstance().getRemoteUser()
        val remotePassword = ChutneySettings.getInstance().getRemotePassword()

        val encodedAuth = Base64.getEncoder().encodeToString("$remoteUser:$remotePassword".toByteArray())
        val authHeaderValue = "Basic $encodedAuth"
        val connection = url.openConnection(Proxy.NO_PROXY) as HttpURLConnection
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.setRequestProperty("Authorization", authHeaderValue)
        connection.doOutput = true
        connection.doInput = true
        connection.requestMethod = requestMethod
        CertificateManager.getInstance().sslContext
        if (connection is HttpsURLConnection) {
            this.configureSslConnection(query, connection)
        }

        if (body.isNotBlank()) {
            val os = connection.outputStream
            os.write(body.toByteArray())
            os.close()
        }
        try {
            val inputStream = BufferedInputStream(connection.inputStream)
            val reader: Reader = InputStreamReader(inputStream, "UTF-8")
            return Gson().fromJson(reader, T::class.java)
        } catch (e: Exception) {
            throw e
        }
    }

    inline fun configureSslConnection(url: String, connection: HttpsURLConnection) {
        if (ApplicationManager.getApplication() == null) {
            LOG.info("Application is not initialized yet; Using default SSL configuration to connect to $url")
        } else {
            try {
                val factory = CertificateManager.getInstance().sslContext.socketFactory
                if (factory == null) {
                    LOG.info("SSLSocketFactory is not defined by IDE CertificateManager; Using default SSL configuration to connect to $url")
                } else {
                    connection.sslSocketFactory = factory
                }
            } catch (var3: Throwable) {
                LOG.info("Problems configuring SSL connection to $url", var3)
            }

        }
        connection.setHostnameVerifier { hostname, sslSession -> true }
    }
}
