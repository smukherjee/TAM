-- Minimal demo seed to avoid empty charts on fresh installs
-- Forecast violations hourly: add a few rows for two tenants
INSERT INTO public.forecast_violations_hourly (hour, tenant_code, expected_count, lower, upper, created_at)
VALUES
  (date_trunc('hour', now()) - interval '1 hour', 'VIDP', 7, 3, 12, now()),
  (date_trunc('hour', now()), 'VIDP', 9, 4, 15, now()),
  (date_trunc('hour', now()) - interval '1 hour', 'YBBN', 4, 1, 8, now()),
  (date_trunc('hour', now()), 'YBBN', 6, 2, 11, now())
ON CONFLICT DO NOTHING;

-- Optional: a single repeat offender and stand conflict examples (if tables exist)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='pred_asset_violation_risk') THEN
    INSERT INTO public.pred_asset_violation_risk (tenant_code, asset_identifier, probability, expected_severity, as_of, created_at)
    VALUES ('VIDP', 'VEH-001', 0.78, 'High', now(), now())
    ON CONFLICT DO NOTHING;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='pred_congestion') THEN
    INSERT INTO public.pred_congestion (tenant_code, zone, forecast_time, expected_density, created_at)
    VALUES ('VIDP', 'ZONE-A1', now() + interval '1 hour', 0.66, now())
    ON CONFLICT DO NOTHING;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='pred_zone_breach') THEN
    INSERT INTO public.pred_zone_breach (tenant_code, zone_id, horizon_minutes, probability, top_asset_categories, as_of)
    VALUES ('VIDP', '00000000-0000-0000-0000-000000000000', 60, 0.42, ARRAY['baggage','tug'], now())
    ON CONFLICT DO NOTHING;
  END IF;
END$$;
