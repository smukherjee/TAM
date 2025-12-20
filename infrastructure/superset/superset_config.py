
# Superset Configuration for TAM Platform (Dev)

ROW_LIMIT = 5000
SECRET_KEY = 'tam-dev-secret-key-change-in-prod'

# Feature Flags
FEATURE_FLAGS = {
    "EMBEDDED_SUPERSET": True,
    "DASHBOARD_NATIVE_FILTERS": True,
    "DASHBOARD_CROSS_FILTERS": True,
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

# Session
SESSION_COOKIE_SAMESITE = None
