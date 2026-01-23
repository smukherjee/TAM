#!/bin/bash
# Verify Database Schema Refactoring
# Checks for existence of tables, seed data, hypertables, and specific columns.

DB_CONTAINER="tam-timescaledb-1"
DB_USER="postgres"
DB_NAME="utam"

echo "========================================================"
echo "   TAM Database Schema Verification"
echo "========================================================"

echo -e "\n[1] Checking Table Existence (Should see 13 tables)..."
docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "\dt"

echo -e "\n[2] Checking Seeded Tenants (Should see VIDP, LIRN, and YBBN)..."
docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "SELECT code, name, timezone FROM tenants;"

echo -e "\n[3] Checking Seeded Users (Should see 6 users linked to tenants)..."
docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "SELECT username, role, tenant_code FROM users ORDER BY tenant_code;"

echo -e "\n[4] Checking TimescaleDB Hypertables..."
docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "SELECT hypertable_name FROM timescaledb_information.hypertables ORDER BY hypertable_name;"

echo -e "\n[5] Checking Materialized Views..."
docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "SELECT view_name FROM timescaledb_information.continuous_aggregates;"

echo -e "\n[6] Verifying 'flights' table structure (Check for UUID id and NO tenant_code FK)..."
docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "\d flights"

echo -e "\n[7] Verifying 'audit_logs' table structure..."
docker exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME -c "\d audit_logs"

echo -e "\n========================================================"
echo "Verification Complete."
