CREATE TABLE complaints (
    id                    BIGSERIAL PRIMARY KEY,
    title                 VARCHAR(200)  NOT NULL,
    description           TEXT          NOT NULL,
    category              VARCHAR(30)   NOT NULL,
    status                VARCHAR(20)   NOT NULL,
    priority              VARCHAR(20)   NOT NULL,
    user_id               BIGINT        NOT NULL,
    faculty               VARCHAR(50),
    dormitory             VARCHAR(100),
    building              VARCHAR(100),
    location_text         VARCHAR(255),
    latitude              DOUBLE PRECISION,
    longitude             DOUBLE PRECISION,
    assigned_department   VARCHAR(100),
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    resolved_at           TIMESTAMP
);

CREATE INDEX idx_complaints_status   ON complaints (status);
CREATE INDEX idx_complaints_category ON complaints (category);
CREATE INDEX idx_complaints_user     ON complaints (user_id);