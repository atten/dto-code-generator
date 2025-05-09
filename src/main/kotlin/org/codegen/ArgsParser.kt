package org.codegen

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import java.io.File

class ArgsParser(
    private val input: Array<String>,
) {
    fun parse(): Args? {
        val params = Args()
        val parser = JCommander(Args())
        parser.programName = AppConfiguration.name

        try {
            JCommander.newBuilder().addObject(params).build().parse(*input)
        } catch (e: ParameterException) {
            println(e.toString())
            parser.usage()
            return null
        }
        if (params.help) {
            parser.usage()
            return null
        }
        if (params.version) {
            parser.console.println(AppConfiguration.version)
            return null
        }

        params.inputPaths = extractFiles(params.inputPaths)

        return params
    }

    private fun extractFiles(list: List<String>): List<String> {
        // include input files / directories
        val files = mutableListOf<String>()
        for (path in list) {
            val filePaths = File(path).listFiles()?.map { it.path }?.filter { File(it).listFiles() == null }?.sorted()
            if (filePaths != null) {
                // path is a directory
                files += filePaths
            } else {
                // path is file
                files.add(path)
            }
        }
        return files
    }
}
