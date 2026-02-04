-- Predictive report tables scaffolding
-- Date: 2026-02-04
-- Purpose: Create tables to store model-scored predictions for Superset dashboards

BEGIN;

CREATE TABLE IF NOT EXISTS pred_turnaround_risk (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  tenant_code VARCHAR(4) NOT NULL,
  flight_id VARCHAR(50) NOT NULL,
  stand_id VARCHAR(50),
  risk_score DOUBLE PRECISION NOT NULL,
  risk_band VARCHAR(20) NOT NULL, -- LOW/MEDIUM/HIGH/CRITICAL
  driver_feature_json JSONB,
  as_of TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pred_turnaround_risk_tenant_asof
  ON pred_turnaround_risk (tenant_code, as_of DESC);

CREATE TABLE IF NOT EXISTS pred_congestion (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  tenant_code VARCHAR(4) NOT NULL,
  zone VARCHAR(100) NOT NULL,
  forecast_time TIMESTAMPTZ NOT NULL,
  expected_density DOUBLE PRECISION NOT NULL,
  model_info JSONB,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pred_congestion_tenant_time
  ON pred_congestion (tenant_code, forecast_time DESC);

CREATE TABLE IF NOT EXISTS pred_zone_breach (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  tenant_code VARCHAR(4) NOT NULL,
  zone_id UUID NOT NULL,
  horizon_minutes INTEGER NOT NULL,
  probability DOUBLE PRECISION NOT NULL,
  top_asset_categories TEXT[],
  as_of TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pred_zone_breach_tenant_asof
  ON pred_zone_breach (tenant_code, as_of DESC);

CREATE TABLE IF NOT EXISTS pred_asset_violation_risk (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  tenant_code VARCHAR(4) NOT NULL,
  asset_identifier VARCHAR(50) NOT NULL,
  probability DOUBLE PRECISION NOT NULL,
  expected_severity VARCHAR(20),
  as_of TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pred_asset_violation_risk_tenant_asof
  ON pred_asset_violation_risk (tenant_code, as_of DESC);

CREATE TABLE IF NOT EXISTS forecast_violations_hourly (
  id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
  tenant_code VARCHAR(4) NOT NULL,
  hour TIMESTAMPTZ NOT NULL,
  expected_count INTEGER NOT NULL,
  lower INTEGER,
  upper INTEGER,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_forecast_violations_hourly_tenant_hour
  ON forecast_violations_hourly (tenant_code, hour DESC);

COMMIT;
