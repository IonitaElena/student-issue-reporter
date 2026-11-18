CREATE TABLE complaint_votes (
    id            BIGSERIAL PRIMARY KEY,
    complaint_id  BIGINT    NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    user_id       BIGINT    NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vote_complaint_user UNIQUE (complaint_id, user_id)
);

CREATE TABLE complaint_comments (
    id            BIGSERIAL PRIMARY KEY,
    complaint_id  BIGINT        NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    user_id       BIGINT        NOT NULL,
    author_email  VARCHAR(255)  NOT NULL,
    content       VARCHAR(1000) NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_votes_complaint    ON complaint_votes (complaint_id);
CREATE INDEX idx_comments_complaint ON complaint_comments (complaint_id);