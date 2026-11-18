CREATE TABLE complaint_status_history (
    id            BIGSERIAL PRIMARY KEY,
    complaint_id  BIGINT       NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    old_status    VARCHAR(20),
    new_status    VARCHAR(20)  NOT NULL,
    changed_by    BIGINT       NOT NULL,
    comment       VARCHAR(500),
    changed_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_status_history_complaint ON complaint_status_history (complaint_id);