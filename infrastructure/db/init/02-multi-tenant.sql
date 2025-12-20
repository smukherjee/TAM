-- Multi-Tenancy Migration
ALTER TABLE flights ADD COLUMN IF NOT EXISTS icao_code VARCHAR(4);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS icao_code VARCHAR(4);
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS icao_code VARCHAR(4);
ALTER TABLE turnaround_events ADD COLUMN IF NOT EXISTS icao_code VARCHAR(4);

-- Users & Roles
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    icao_code VARCHAR(4),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed Data (Use ON CONFLICT DO NOTHING to avoid errors on rerun)
INSERT INTO users (username, password, role, icao_code) VALUES
('admin_vidp', 'admin', 'ADMIN', 'VIDP'),
('gh_vidp', 'gh', 'GH', 'VIDP'),
('user_vidp', 'user', 'AIRPORT_USER', 'VIDP'),
('admin_lirn', 'admin', 'ADMIN', 'LIRN'),
('gh_lirn', 'gh', 'GH', 'LIRN'),
('user_lirn', 'user', 'AIRPORT_USER', 'LIRN')
ON CONFLICT (username) DO NOTHING;
