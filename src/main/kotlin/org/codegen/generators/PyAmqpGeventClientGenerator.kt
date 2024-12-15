package org.codegen.generators

class PyAmqpGeventClientGenerator(proxy: AbstractCodeGenerator? = null) : PyAmqpBlockingClientGenerator(proxy) {
    override fun requiredHeaders() =
        listOf(
            "import io",
            "import os",
            "import json",
            "from dataclasses import is_dataclass",
            "from dataclasses import astuple",
            "from dataclasses import asdict",
            "from dataclasses import dataclass",
            "import marshmallow_dataclass",
            "from datetime import datetime",
            "from datetime import timedelta",
            "from datetime import timezone",
            "from decimal import Decimal",
            "import marshmallow",
            "import ijson",
            "import logging",
            "import time",
            "from funcy import memoize",
            "import gevent",
            "from uuid import uuid4",
            "from gevent._semaphore import Semaphore",
            "from gevent.event import AsyncResult",
            "from urllib.parse import urlparse",
            "from amqp.exceptions import RecoverableConnectionError, ConnectionForced",
            "from kombu import Connection, Exchange, Queue, Message",
            "from socket import timeout as SocketTimeout",
            "from typeguard import typechecked",
        )

    override fun getMainApiClassBody() = super.getMainApiClassBody().replace("AmqpApiWithBlockingListener", "AmqpApiWithLazyListener")

    override fun buildBodyPostfix(): String {
        addDefinition("", "AmqpApiWithLazyListener") // make listener class accessible from outside
        return super.buildBodyPostfix()
    }

    override fun getIncludedIntoFooterPaths() =
        listOf(
            "/templates/python/baseJsonAmqpGeventClient.py",
            "/templates/python/baseSerializer.py",
            "/templates/python/baseDeserializer.py",
            "/templates/python/failsafeCall.py",
        )
}
