CREATE TABLE complaint_images (
    id                BIGSERIAL PRIMARY KEY,
    complaint_id      BIGINT       NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(100) NOT NULL UNIQUE,
    content_type      VARCHAR(50)  NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    uploaded_by       BIGINT       NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_images_complaint ON complaint_images (complaint_id);