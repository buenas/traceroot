INSERT INTO log_record (
    id,
    level,
    service_name,
    message,
    timestamp,
    environment,
    trace_id,
    endpoint,
    exception_type,
    version,
    created_at
)
VALUES (
           gen_random_uuid(),
           'ERROR',
           'payments-service',
           'Stripe timeout',
           NOW(),
           'prod',
           'abc-123',
           '/charge',
           'TimeoutException',
           '1.0.0',
           NOW()
       );

INSERT INTO log_record (
    id,
    level,
    service_name,
    message,
    timestamp,
    environment,
    trace_id,
    endpoint,
    exception_type,
    version,
    created_at
)
VALUES (
           gen_random_uuid(),
           'ERROR',
           'currency-exchange-service',
           'Value timeout',
           NOW(),
           'prod',
           'abc-123',
           '/currency-exchange',
           'NullPointerException',
           '1.0.0',
           NOW()
       );