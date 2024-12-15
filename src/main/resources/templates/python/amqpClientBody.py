    @typechecked
    def __init__(
        self,
        amqp_url: str,
        read_exchange_name: str,
        read_queue_name: str,
        write_exchange_name: str = None,
        prefetch_count: int = 30,
        logger: logging.Logger = None,
        api_keys: dict = None,
        high_priority: bool = False,
        request_timeout: int = 3600,
        use_request_payload_validation: bool = bool(int(os.environ.get('AMQP_CLIENT_USE_REQUEST_PAYLOAD_VALIDATION', 1))),
    ):
        """
        AMQP client constructor and configuration method.
        """
        self._client = AmqpApiWithBlockingListener(
            amqp_url=amqp_url,
            read_exchange_name=read_exchange_name,
            read_queue_name=read_queue_name,
            write_exchange_name=write_exchange_name,
            prefetch_count=prefetch_count,
            logger=logger,
            api_keys=api_keys,
            high_priority=high_priority,
            request_timeout=request_timeout
        )

        self._deserializer = BaseDeserializer(
            use_response_streaming=False
        )

        self._serializer = BaseSerializer(
            self._deserializer,
            use_request_payload_validation=use_request_payload_validation
        )
