package org.codegen.generators

class PyAmqpGeventClientGenerator(proxy: AbstractCodeGenerator? = null) : PyAmqpBlockingClientGenerator(proxy) {
    override fun renderHeaders(): String {
        listOf(
            "from dataclasses import asdict",
            "import gevent",
            "from gevent._semaphore import Semaphore",
            "from gevent.event import AsyncResult",
        ).forEach { headers.add(it) }

        return super.renderHeaders()
            .replace("\nfrom threading import Lock", "")
    }

    override fun getMainApiClassBody() =
        super.getMainApiClassBody()
            .replace("AmqpApiWithBlockingListener", "AmqpApiWithLazyListener")
            .trimEnd()

    override fun getBodyIncludedFiles(): List<String> {
        val original = super.getBodyIncludedFiles().toMutableList()
        original.replaceAll {
            mapOf(
                "resource:/templates/python/baseJsonAmqpBlockingClient.py" to "resource:/templates/python/baseJsonAmqpGeventClient.py",
            ).getOrDefault(it, it)
        }
        return original
    }

    override fun renderBodyPrefix(): String {
        definedNames.add("AmqpApiWithLazyListener") // make listener class accessible from outside
        return super.renderBodyPrefix()
    }
}
