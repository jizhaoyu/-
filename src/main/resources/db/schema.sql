CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT PRIMARY KEY,
    kb_id VARCHAR(128) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tags VARCHAR(1024),
    error_message VARCHAR(1024),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    indexed_at TIMESTAMP,
    INDEX idx_kb_document_kb_id (kb_id),
    INDEX idx_kb_document_status (status)
);
