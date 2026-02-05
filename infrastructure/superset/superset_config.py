
# Superset Configuration for TAM Platform (Dev)

ROW_LIMIT = 5000
SECRET_KEY = 'tam-dev-secret-key-change-in-prod'

# Database Connection (Metadata)
import os
SQLALCHEMY_DATABASE_URI = os.environ.get("SQLALCHEMY_DATABASE_URI", "postgresql://postgres:password@timescaledb:5432/utam")

# Feature Flags
FEATURE_FLAGS = {
    "EMBEDDED_SUPERSET": True,
    "DASHBOARD_NATIVE_FILTERS": True,
    "DASHBOARD_CROSS_FILTERS": True,
    "ENABLE_TEMPLATE_PROCESSING": True,
}

# CORS & Embedding
ENABLE_CORS = True
CORS_OPTIONS = {
    'supports_credentials': True,
    'allow_headers': ['*'],
    'resources': {'*': {'origins': ['http://localhost:3000', 'http://127.0.0.1:3000']}},
}

# Allow iframes (Disable X-Frame-Options SAMEORIGIN)
TALISMAN_ENABLED = False
HTTP_HEADERS = {'X-Frame-Options': 'ALLOWALL'}
OVERRIDE_HTTP_HEADERS = {'X-Frame-Options': 'ALLOWALL'}

# Disable CSRF for easier dev integration
WTF_CSRF_ENABLED = False

# Disable public/guest access for now to avoid AnonymousUser confusion during provisioning
# PUBLIC_ROLE_LIKE = "Gamma"
# AUTH_ROLE_PUBLIC = "Public"

# Ensure JWT authentication is correctly handled
FAB_API_SWAGGER_UI = True

# Session settings for dev
SESSION_COOKIE_HTTPONLY = True
SESSION_COOKIE_SECURE = False
SESSION_COOKIE_SAMESITE = 'Lax'
