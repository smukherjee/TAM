import sys
import os
import json
import uuid
import traceback

# Ensure Flask app context
os.environ['FLASK_APP'] = 'superset'
from superset.app import create_app

def log(msg):
    print(msg, flush=True)

def provision():
    try:
        app = create_app()
        with app.app_context():
            from superset import db
            from superset.models.core import Database
            from superset.models.slice import Slice
            from superset.models.dashboard import Dashboard
            from superset.connectors.sqla.models import SqlaTable
            from flask_appbuilder.security.sqla.models import User

            def get_admin():
                return db.session.query(User).filter_by(username='admin').first()

            def get_or_create_database():
                db_name = "TimescaleDB"
                uri = "postgresql+psycopg2://postgres:password@timescaledb:5432/utam"
                
                database = db.session.query(Database).filter_by(database_name=db_name).first()
                if not database:
                    log(f"Creating database: {db_name}")
                    database = Database(
                        database_name=db_name,
                        sqlalchemy_uri=uri,
                        expose_in_sqllab=True,
                        allow_run_async=True
                    )
                    db.session.add(database)
                    db.session.commit()
                else:
                    log(f"Database exists: {db_name}")
                return database

            def get_or_create_dataset(database, table_name, schema="public", sql=None):
                dataset = db.session.query(SqlaTable).filter_by(
                    table_name=table_name, 
                    database_id=database.id
                ).first()
                
                if not dataset:
                    log(f"Creating dataset: {table_name}")
                    dataset = SqlaTable(
                        table_name=table_name,
                        schema=schema,
                        database=database,
                        sql=sql
                    )
                    db.session.add(dataset)
                    db.session.commit()
                    try:
                        dataset.fetch_metadata()
                        db.session.commit()
                    except Exception as e:
                        log(f"Warning: Failed to fetch metadata for {table_name}: {e}")
                return dataset

            def get_or_create_chart(dataset, slice_name, viz_type, params):
                chart = db.session.query(Slice).filter_by(slice_name=slice_name).first()
                
                # Ensure params is valid JSON string
                if isinstance(params, dict):
                    params = json.dumps(params)
                    
                if not chart:
                    log(f"Creating chart: {slice_name}")
                    chart = Slice(
                        slice_name=slice_name,
                        datasource_type='table',
                        datasource_id=dataset.id,
                        viz_type=viz_type,
                        params=params,
                        owners=[get_admin()]
                    )
                    db.session.add(chart)
                    db.session.commit()
                return chart

            def add_row(layout, row_idx, charts):
                row_id = f"ROW-{row_idx}"
                col_ids = [f"COL-{row_idx}-{i+1}" for i in range(len(charts))]
                
                # Add Row to Grid
                layout["GRID_ID"]["children"].append(row_id)
                layout[row_id] = {"id": row_id, "type": "ROW", "children": col_ids}
                
                for i, chart in enumerate(charts):
                    col_id = col_ids[i]
                    chart_node_id = f"CHART-{chart.id}"
                    
                    # Add Col to Layout
                    layout[col_id] = {
                        "id": col_id, 
                        "type": "COLUMN", 
                        "children": [chart_node_id],
                        "meta": {"width": 6} # 12-grid system, 2 items = width 6
                    }
                    
                    # Add Chart to Layout
                    layout[chart_node_id] = {
                        "id": chart_node_id,
                        "type": "CHART",
                        "meta": {
                            "chartId": chart.id,
                            "sliceName": chart.slice_name,
                            "uuid": str(uuid.uuid4())
                        }
                    }

            def get_or_create_dashboard(title, slug, charts):
                dashboard = db.session.query(Dashboard).filter_by(dashboard_title=title).first()
                
                if not dashboard:
                    log(f"Creating dashboard: {title}")
                    dashboard = Dashboard(
                        dashboard_title=title,
                        slug=slug,
                        owners=[get_admin()],
                        published=True
                    )
                    
                    # Build Grid Layout
                    layout = {
                        "DASHBOARD_VERSION": "v2",
                        "ROOT_ID": {"id": "ROOT_ID", "type": "ROOT", "children": ["HEADER_ID", "GRID_ID"]},
                        "HEADER_ID": {"id": "HEADER_ID", "type": "HEADER", "meta": {"text": title}},
                        "GRID_ID": {"id": "GRID_ID", "type": "GRID", "children": []}
                    }
                    
                    # Add charts in rows of 2
                    buffer = []
                    row_idx = 0
                    
                    for chart in charts:
                        buffer.append(chart)
                        if len(buffer) == 2:
                            row_idx += 1
                            add_row(layout, row_idx, buffer)
                            buffer = []
                    
                    # Add remaining chart
                    if buffer:
                        row_idx += 1
                        add_row(layout, row_idx, buffer)
                    
                    dashboard.position_json = json.dumps(layout)
                    dashboard.json_metadata = json.dumps({})
                    
                    # Link slices
                    dashboard.slices = charts
                    
                    db.session.add(dashboard)
                    db.session.commit()
                    log(f"Dashboard created: {title} (ID: {dashboard.id})")
                else:
                    log(f"Dashboard exists: {title}")
                
                return dashboard

            # --- EXECUTION ---
            database = get_or_create_database()
            
            # --- Datasets ---
            datasets = {}
            dataset_names = [
                "v_ops_overview_daily", "v_flight_movements_hourly", "v_vehicle_activity_summary_daily",
                "v_stand_gate_occupancy", "v_turnaround_sla_compliance", "v_delay_root_causes",
                "v_speed_violations_by_zone", "v_restricted_zone_breach_dwell", "v_discrepancy_trends_daily",
                "v_asset_utilization_status_counts", "v_maintenance_downtime_by_type", "v_dwell_proxy_by_zone_hourly",
                "v_alerts_summary_type_hour", "v_repeat_offenders_assets", "v_throughput_ops_volume_today",
                "v_pipeline_health_events_per_minute", "v_activity_heatmap_latest", "v_violation_heatmap_latest",
                "v_stand_conflicts", "pred_turnaround_risk", "pred_congestion", "pred_zone_breach",
                "pred_asset_violation_risk", "forecast_violations_hourly"
            ]
            
            for name in dataset_names:
                datasets[name] = get_or_create_dataset(database, name)
                
            # --- Charts ---
            charts = {}
            
            def create_table_chart(name, ds_name, cols, time_range="No filter"):
                params = {
                    "all_columns": cols,
                    "query_mode": "raw",
                    "row_limit": 500,
                    "time_range": time_range,
                    "include_search": True,
                    "show_cell_bars": True
                }
                return get_or_create_chart(datasets[ds_name], name, "table", params)

            # OPS
            charts["CH_OPS_F"] = create_table_chart("Ops Overview Daily", "v_ops_overview_daily", ["tenant_code", "day", "flights", "vehicles", "alerts", "violations"])
            charts["CH_FLT_HR"] = create_table_chart("Flight Movements Hourly", "v_flight_movements_hourly", ["tenant_code", "hour", "positions"])
            charts["CH_VEH_SUM"] = create_table_chart("Vehicle Activity Daily", "v_vehicle_activity_summary_daily", ["tenant_code", "day", "vehicle_type", "telemetry_points", "avg_speed"])
            
            # Line Charts (Custom params)
            p_line_flt = {
                "metrics": [{"expressionType": "SQL", "sqlExpression": "sum(positions)", "label": "Positions"}],
                "granularity_sqla": "hour", 
                "time_grain_sqla": "PT1H", 
                "time_range": "last 24 hours"
            }
            charts["CH_LINE_FLT"] = get_or_create_chart(datasets["v_flight_movements_hourly"], "Flight Movements (Line)", "line", p_line_flt)
            
            charts["CH_THRPT"] = create_table_chart("Throughput Today", "v_throughput_ops_volume_today", ["tenant_code", "flights_today", "tasks_done_today", "alerts_closed_today"])

            # SAFETY
            charts["CH_VIOL_ZONE"] = create_table_chart("Violations by Zone", "v_speed_violations_by_zone", ["tenant_code", "zone_name", "severity", "violations"])
            charts["CH_BREACH"] = create_table_chart("Breach Dwell Stats", "v_restricted_zone_breach_dwell", ["tenant_code", "zone_name", "zone_type", "avg_dwell_sec", "max_dwell_sec", "breaches"])
            charts["CH_DISCREP"] = create_table_chart("Discrepancy Trends Daily", "v_discrepancy_trends_daily", ["tenant_code", "discrepancy_type", "day", "discrepancies"])
            charts["CH_OFFEND"] = create_table_chart("Repeat Offenders", "v_repeat_offenders_assets", ["tenant_code", "asset_identifier", "violations"])
            
            # DeckGL
            p_deck_viol = {
                "spatial": {"type": "latlon", "latCol": "grid_latitude", "lonCol": "grid_longitude"},
                "mapbox_style": "mapbox://styles/mapbox/dark-v10",
                "point_radius_fixed": 20,
                "row_limit": 5000,
                "time_grain_sqla": "PT1H",
                "granularity_sqla": "time_bucket",
                "weight": "violation_count",
                "metric": {"expressionType": "SQL", "sqlExpression": "sum(violation_count)", "label": "Violations"}
            }
            charts["CH_DECK_VIOL"] = get_or_create_chart(datasets["v_violation_heatmap_latest"], "Violation Heatmap (deck.gl)", "deck_heatmap", p_deck_viol)
            
            p_line_alerts = {
                "metrics": [{"expressionType": "SQL", "sqlExpression": "sum(alerts)", "label": "Alerts"}],
                "granularity_sqla": "hour",
                "time_grain_sqla": "PT1H",
                "time_range": "last 24 hours",
                "groupby": ["type"]
            }
            charts["CH_LINE_ALERTS"] = get_or_create_chart(datasets["v_alerts_summary_type_hour"], "Alerts by Type (Line)", "line", p_line_alerts)

            # TURNAROUND
            charts["CH_STAND_OCC"] = create_table_chart("Stand Occupancy", "v_stand_gate_occupancy", ["tenant_code", "stand_id", "sessions", "avg_turnaround_min"])
            charts["CH_SLA"] = create_table_chart("SLA Compliance by Task", "v_turnaround_sla_compliance", ["tenant_code", "task_type", "avg_delay_min", "on_time_ratio", "tasks"])
            charts["CH_DELAY"] = create_table_chart("Delay Root Causes", "v_delay_root_causes", ["tenant_code", "cause", "total_delay_min", "affected_tasks"])
            charts["CH_STAND_CONFLICT"] = create_table_chart("Stand Conflicts", "v_stand_conflicts", ["tenant_code", "stand_id", "overlapping_pairs"])

            # ASSETS
            charts["CH_UTIL"] = create_table_chart("Asset Utilization Status", "v_asset_utilization_status_counts", ["tenant_code", "status", "assets"])
            charts["CH_MAINT"] = create_table_chart("Maintenance Downtime", "v_maintenance_downtime_by_type", ["tenant_code", "status", "count"])
            charts["CH_DWELL"] = create_table_chart("Dwell Proxy by Zone Hourly", "v_dwell_proxy_by_zone_hourly", ["tenant_code", "zone", "hour", "movement_points"])
            
            p_deck_act = {
                "spatial": {"type": "latlon", "latCol": "grid_latitude", "lonCol": "grid_longitude"},
                "mapbox_style": "mapbox://styles/mapbox/dark-v10",
                "point_radius_fixed": 20,
                "row_limit": 5000,
                "time_grain_sqla": "PT1H",
                "granularity_sqla": "time_bucket",
                "weight": "activity_count",
                "metric": {"expressionType": "SQL", "sqlExpression": "sum(activity_count)", "label": "Activity"}
            }
            charts["CH_DECK_ACT"] = get_or_create_chart(datasets["v_activity_heatmap_latest"], "Activity Heatmap (deck.gl)", "deck_heatmap", p_deck_act)

            # PIPELINE
            charts["CH_PIPE"] = create_table_chart("Pipeline Events/min", "v_pipeline_health_events_per_minute", ["tenant_code", "minute", "events"])

            # PREDICTIVE
            charts["CH_PRED_TA"] = create_table_chart("Turnaround Risk", "pred_turnaround_risk", ["tenant_code", "flight_id", "stand_id", "risk_score", "risk_band", "as_of", "created_at"])
            charts["CH_PRED_CONG"] = create_table_chart("Congestion Forecast", "pred_congestion", ["tenant_code", "zone", "forecast_time", "expected_density", "created_at"])
            charts["CH_PRED_ZONE"] = create_table_chart("Zone Breach Probability", "pred_zone_breach", ["tenant_code", "zone_id", "horizon_minutes", "probability", "top_asset_categories", "as_of"])
            charts["CH_PRED_ASSET"] = create_table_chart("Asset Violation Risk", "pred_asset_violation_risk", ["tenant_code", "asset_identifier", "probability", "expected_severity", "as_of", "created_at"])
            charts["CH_FORE_VIOL"] = create_table_chart("Violations Forecast (Hourly)", "forecast_violations_hourly", ["hour", "tenant_code", "expected_count", "lower", "upper", "created_at"])

            # --- Dashboards ---
            
            def execute_sql_file(file_path):
                if not os.path.exists(file_path):
                    log(f"Warning: Seed file not found at {file_path}")
                    return
                
                log(f"Seeding data from {file_path}")
                with open(file_path, 'r') as f:
                    sql_content = f.read()
                    # Execute raw SQL
                    # Note: text() is needed for SQLAlchemy
                    from sqlalchemy import text
                    try:
                        # Split by semi-colon to handle multiple statements generally, 
                        # but psql features might break. Assuming simple SQL.
                        # Actually standard SQL alchemy execute might handle script if properly formatted
                        # But simpler to just run it. Using connection.
                        connection = db.engine.raw_connection()
                        try:
                            cursor = connection.cursor()
                            cursor.execute(sql_content)
                            connection.commit()
                            log("Seeding completed.")
                        finally:
                            connection.close()
                    except Exception as e:
                        log(f"Error executing seed file: {e}")

            def refresh_mat_views():
                views = ["asset_activity_heatmap", "violation_heatmap"]
                for v in views:
                    try:
                        log(f"Refreshing materialized view: {v}")
                        db.session.execute(f"REFRESH MATERIALIZED VIEW {v}")
                        db.session.commit()
                    except Exception as e:
                        log(f"Error refreshing {v}: {e}")

            # --- Dashboards ---
            
            get_or_create_dashboard(
                "TAM Ops Overview", "tam_ops_full", 
                [charts["CH_OPS_F"], charts["CH_FLT_HR"], charts["CH_VEH_SUM"], charts["CH_THRPT"], charts["CH_LINE_FLT"]]
            )
            
            get_or_create_dashboard(
                "TAM Safety & Security", "tam_safety",
                [charts["CH_VIOL_ZONE"], charts["CH_BREACH"], charts["CH_DISCREP"], charts["CH_OFFEND"], charts["CH_DECK_VIOL"], charts["CH_LINE_ALERTS"]]
            )
            
            get_or_create_dashboard(
                "TAM Turnaround", "tam_turnaround",
                [charts["CH_STAND_OCC"], charts["CH_SLA"], charts["CH_DELAY"], charts["CH_STAND_CONFLICT"]]
            )
            
            get_or_create_dashboard(
                "TAM Assets", "tam_assets",
                [charts["CH_UTIL"], charts["CH_MAINT"], charts["CH_DWELL"], charts["CH_DECK_ACT"]]
            )
            
            get_or_create_dashboard(
                "TAM Pipeline", "tam_pipeline",
                [charts["CH_PIPE"]]
            )
            
            get_or_create_dashboard(
                "TAM Predictive", "tam_predictive",
                [charts["CH_FORE_VIOL"], charts["CH_PRED_TA"], charts["CH_PRED_CONG"], charts["CH_PRED_ZONE"], charts["CH_PRED_ASSET"]]
            )
            
            # --- Seeding & Refresh ---
            execute_sql_file("/seeds/11-demo-seed.sql")
            refresh_mat_views()
            
            log("Provisioning completed successfully!")

    except Exception as e:
        log(f"Error: {e}")
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    provision()
