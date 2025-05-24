package org.codegen.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object Reader {
    var insecureHttps = false

    fun readFileOrResourceOrUrl(
        pathOrUrl: String,
        pathRelativeTo: String? = null,
    ): String {
        if (pathOrUrl.lowercase().startsWith("http")) {
            return if (insecureHttps) readUrlInsecure(pathOrUrl) else readUrl(pathOrUrl)
        }
        if (pathOrUrl.lowercase().startsWith("resource:")) {
            return readResource(pathOrUrl)
        }

        if (pathRelativeTo != null) {
            return readFile(File(pathRelativeTo).resolveSibling(pathOrUrl).absolutePath)
        }

        return readFile(pathOrUrl)
    }

    private fun readFile(path: String): String {
        return File(path).readText()
    }

    private fun readUrl(url: String): String {
        val client = OkHttpClient()
        val request =
            Request.Builder()
                .url(url)
                .build()

        return client.newCall(request).execute().body!!.string()
    }

    private fun readUrlInsecure(url: String): String {
        val trustAllCerts =
            arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String,
                    ) {
                    }

                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String,
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                },
            )
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val client =
            OkHttpClient.Builder()
                .hostnameVerifier { _, _ -> true }
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .build()

        val request =
            Request.Builder()
                .url(url)
                .build()

        return client.newCall(request).execute().body!!.string()
    }

    private fun readResource(uri: String): String {
        val resourcePath = uri.replace("resource:", "")
        val stream = this.javaClass.getResourceAsStream(resourcePath)
        requireNotNull(stream) { "Resource not found: $resourcePath" }
        return stream.bufferedReader().readText()
    }
}
