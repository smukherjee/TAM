-- Wildlife Hazard Sightings/Strikes
-- Date: 2026-07-06
-- AOM Ch 6.2 (Wildlife Hazard Management Programme). Same convention as
-- 25-ppe-density-walkway-safety.sql: new table + deterministic md5-backfill mock data.

BEGIN;

CREATE TABLE IF NOT EXISTS wildlife_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    species VARCHAR(100) NOT NULL,
    zone VARCHAR(100) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    "timestamp" TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_wildlife_events_tenant ON wildlife_events (tenant_code, "timestamp" DESC);

COMMENT ON TABLE wildlife_events IS 'Wildlife sightings, strikes and dispersal actions on the airside, for AOM Ch 6.2 Wildlife Hazard Management Programme reporting.';

CREATE OR REPLACE VIEW v_wildlife_hazard_summary AS
SELECT
    tenant_code,
    species,
    zone,
    date_trunc('month', "timestamp") AS month,
    count(*) AS events,
    sum(CASE WHEN event_type = 'STRIKE' THEN 1 ELSE 0 END) AS strikes,
    sum(CASE WHEN event_type = 'SIGHTING' THEN 1 ELSE 0 END) AS sightings,
    sum(CASE WHEN event_type = 'DISPERSAL_ACTION' THEN 1 ELSE 0 END) AS dispersal_actions,
    sum(CASE WHEN severity = 'HIGH' THEN 1 ELSE 0 END) AS high_severity
FROM wildlife_events
GROUP BY 1, 2, 3, 4;

COMMENT ON VIEW v_wildlife_hazard_summary IS 'Monthly wildlife hazard activity by species/zone, for AOM Ch 6.2 reporting.';

COMMIT;

-- =====================================================================
-- Mock data: deterministic on a synthetic row key, idempotent to re-run.
-- Species mix is tenant-appropriate (Black Kite/feral dogs are the dominant
-- real-world hazard at Indian airports; European gulls at LIRN; ibis/flying-fox
-- at Australian airports).
-- =====================================================================

INSERT INTO wildlife_events (tenant_code, species, zone, event_type, severity, "timestamp")
SELECT
    t.tenant_code,
    t.species,
    (ARRAY['Runway Approach', 'Apron Perimeter', 'Taxiway Edge', 'Grass Strip'])[1 + (gs % 4)],
    CASE ('x' || substr(md5(t.tenant_code || t.species || gs::text || 'type'), 1, 8))::bit(32)::int % 10
        WHEN 0 THEN 'STRIKE'
        WHEN 1 THEN 'DISPERSAL_ACTION'
        ELSE 'SIGHTING'
    END,
    CASE ('x' || substr(md5(t.tenant_code || t.species || gs::text || 'sev'), 1, 8))::bit(32)::int % 10
        WHEN 0 THEN 'HIGH'
        WHEN 1 THEN 'MEDIUM'
        WHEN 2 THEN 'MEDIUM'
        ELSE 'LOW'
    END,
    NOW() - ((gs * 36 + (('x' || substr(md5(t.tenant_code || t.species || gs::text), 1, 8))::bit(32)::int % 24)) || ' hours')::INTERVAL
FROM (VALUES ('VIDP', 'Black Kite'), ('VIDP', 'Feral Dog'), ('VIDP', 'Egret'),
             ('LIRN', 'Yellow-legged Gull'), ('LIRN', 'Kestrel'),
             ('YBBN', 'Australian Ibis'), ('YBBN', 'Flying Fox')) AS t(tenant_code, species)
CROSS JOIN generate_series(1, 40) gs
WHERE NOT EXISTS (SELECT 1 FROM wildlife_events WHERE tenant_code = t.tenant_code AND species = t.species);
