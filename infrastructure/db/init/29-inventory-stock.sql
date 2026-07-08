-- GSE/Consumables Inventory Stock Levels
-- Date: 2026-07-06
-- AOM Ch 3.1 (equipment/consumables readiness). Same convention as
-- 25-ppe-density-walkway-safety.sql: new table + deterministic md5-backfill mock data.

BEGIN;

CREATE TABLE IF NOT EXISTS inventory_stock (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_code VARCHAR(4) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    quantity_on_hand NUMERIC NOT NULL,
    reorder_level NUMERIC NOT NULL,
    last_restocked_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_inventory_stock_item ON inventory_stock (tenant_code, item_name);

COMMENT ON TABLE inventory_stock IS 'GSE consumables and safety-stock quantities (de-icing fluid, PPE, spares), for AOM Ch 3.1 equipment-readiness reporting.';

CREATE OR REPLACE VIEW v_inventory_stock_status AS
SELECT
    tenant_code,
    item_name,
    category,
    unit,
    quantity_on_hand,
    reorder_level,
    CASE WHEN quantity_on_hand <= reorder_level THEN 'REORDER' ELSE 'OK' END AS stock_status,
    last_restocked_at
FROM inventory_stock;

COMMENT ON VIEW v_inventory_stock_status IS 'Reorder-alert view over GSE consumables/spares stock, for AOM Ch 3.1 equipment-readiness reporting.';

COMMIT;

-- =====================================================================
-- Mock data: deterministic on (tenant, item) via md5, idempotent to re-run.
-- ~1 in 5 items seeded below reorder level so the status view always has alerts.
-- =====================================================================

INSERT INTO inventory_stock (tenant_code, item_name, category, unit, quantity_on_hand, reorder_level, last_restocked_at)
SELECT
    t.tenant_code,
    i.item_name,
    i.category,
    i.unit,
    CASE WHEN ('x' || substr(md5(t.tenant_code || i.item_name || 'low'), 1, 8))::bit(32)::int % 5 = 0
        THEN i.reorder_level * 0.6
        ELSE i.reorder_level * (1.5 + (('x' || substr(md5(t.tenant_code || i.item_name || 'qty'), 1, 8))::bit(32)::int % 100) / 50.0)
    END AS quantity_on_hand,
    i.reorder_level,
    NOW() - ((('x' || substr(md5(t.tenant_code || i.item_name || 'restock'), 1, 8))::bit(32)::int % 20 + 1) || ' days')::INTERVAL
FROM (VALUES ('VIDP'), ('LIRN'), ('YBBN')) AS t(tenant_code)
CROSS JOIN (VALUES
    ('De-icing Fluid Type I', 'De-icing Fluid', 'Litres', 2000),
    ('De-icing Fluid Type IV', 'De-icing Fluid', 'Litres', 1500),
    ('Hi-Vis Vest', 'PPE', 'Units', 50),
    ('Ear Protection', 'PPE', 'Pairs', 80),
    ('Safety Helmet', 'PPE', 'Units', 40),
    ('Wheel Chocks', 'GSE Spares', 'Pairs', 30),
    ('GPU Cable', 'GSE Spares', 'Units', 15),
    ('Tow Bar Pin Set', 'GSE Spares', 'Units', 20),
    ('Fire Extinguisher (Refill)', 'Emergency', 'Units', 25),
    ('FOD Sweeper Brush Set', 'Ground Support', 'Units', 10)
) AS i(item_name, category, unit, reorder_level)
WHERE NOT EXISTS (
    SELECT 1 FROM inventory_stock s WHERE s.tenant_code = t.tenant_code AND s.item_name = i.item_name
);
