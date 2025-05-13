package org.codegen.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class Reader {
    fun readFileOrResourceOrUrl(
        pathOrUrl: String,
        pathRelativeTo: String? = null,
    ): String {
        if (pathOrUrl.lowercase().startsWith("http")) {
            return readUrl(pathOrUrl)
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

    private fun readResource(uri: String): String {
        val resourcePath = uri.replace("resource:", "")
        val stream = this.javaClass.getResourceAsStream(resourcePath)
        requireNotNull(stream) { "Resource not found: $resourcePath" }
        return stream.bufferedReader().readText()
    }
}
