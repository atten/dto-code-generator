package org.codegen.generators

import java.io.File

class PyAmqpGeventClientGenerator(proxy: AbstractCodeGenerator? = null) : PyAmqpBlockingClientGenerator(proxy) {
    override val baseClassName = "AmqpApiWithLazyListener"

    override fun buildBodyPrefix(): String {
        headers.add("import typing as t")
        headers.add("import io")
        headers.add("import json")
        headers.add("from dataclasses import is_dataclass")
        headers.add("from dataclasses import astuple")
        headers.add("from dataclasses import asdict")
        headers.add("from dataclasses import dataclass")
        headers.add("import marshmallow_dataclass")
        headers.add("from datetime import datetime")
        headers.add("from datetime import timedelta")
        headers.add("from datetime import timezone")
        headers.add("from decimal import Decimal")
        headers.add("import marshmallow")
        headers.add("import ijson")
        headers.add("import logging")
        headers.add("import time")
        headers.add("from funcy import memoize")
        headers.add("import gevent")
        headers.add("from uuid import uuid4")
        headers.add("from gevent._semaphore import Semaphore")
        headers.add("from gevent.event import AsyncResult")
        headers.add("from urllib.parse import urlparse")
        headers.add("from amqp.exceptions import RecoverableConnectionError, ConnectionForced")
        headers.add("from kombu import Connection, Exchange, Queue, Message")
        headers.add("from socket import timeout as SocketTimeout")

        listOf(
            "/templates/python/baseSchema.py",
            "/templates/python/amqpGeventClient.py",
            "/templates/python/serializationMethods.py",
            "/templates/python/failsafeCall.py",
        ).map { path ->
            this.javaClass.getResource(path)!!.path
                .let { File(it).readText() }
        }
            .joinToString(separator = "\n\n")
            .let { addDefinition(it, "FailedAmqpRequestError", "AmqpApiWithLazyListener") }

        return ""
    }
}
