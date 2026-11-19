CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE complaints
    ADD COLUMN location geography(Point, 4326)
    GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED;

CREATE INDEX idx_complaints_location ON complaints USING GIST (location);