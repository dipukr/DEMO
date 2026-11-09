-- For this example the schema is created only if missing.
-- For a production service, version schema changes with Flyway or Liquibase.
IF OBJECT_ID(N'dbo.orders', N'U') IS NULL
CREATE TABLE dbo.orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(80) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(24) NOT NULL,
    created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET()
);

IF OBJECT_ID(N'dbo.idempotency_records', N'U') IS NULL
CREATE TABLE dbo.idempotency_records (
    operation VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    http_status INT,
    response_body NVARCHAR(MAX),
    resource_id BIGINT,
    created_at DATETIMEOFFSET NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    PRIMARY KEY (operation, idempotency_key)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = N'idx_idempotency_records_created_at'
                 AND object_id = OBJECT_ID(N'dbo.idempotency_records'))
CREATE INDEX idx_idempotency_records_created_at
    ON dbo.idempotency_records (created_at);
