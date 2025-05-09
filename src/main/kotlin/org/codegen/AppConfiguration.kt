package org.codegen

import java.util.Properties

object AppConfiguration {
    val name: String by lazy { getProperties().getProperty("name") }
    val version: String by lazy { getProperties().getProperty("version") }

    private fun getProperties(): Properties {
        return getProperties("/application.properties")
    }

    private fun getProperties(path: String): Properties {
        val p = Properties()
        p.load(AppConfiguration.javaClass.getResourceAsStream(path))
        return p
    }
}
