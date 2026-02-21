# Superset Configuration for TAM Platform (Dev)
import json
import re
from urllib.parse import parse_qs, urlparse

from flask import request

ROW_LIMIT = 5000
SECRET_KEY = 'tam-dev-secret-key-change-in-prod'

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

# Session
SESSION_COOKIE_SAMESITE = None

# Enable public/guest access for embedded charts (DEV ONLY)
#PUBLIC_ROLE_LIKE = "Gamma"
#AUTH_ROLE_PUBLIC = "Public"
SQLALCHEMY_DATABASE_URI = 'postgresql+psycopg2://postgres:password@timescaledb:5432/utam'

_TENANT_PATTERN = re.compile(r'^[A-Za-z0-9_-]{2,12}$')


def _sanitize_tenant(value: str) -> str:
    if not value:
        return ''
    candidate = value.strip().upper()
    if _TENANT_PATTERN.match(candidate):
        return candidate
    return ''


def _append_tenant_candidates(candidates: list, data) -> None:
    if not data:
        return
    if isinstance(data, str):
        candidates.append(data)
        return
    if not isinstance(data, dict):
        return

    candidates.extend([
        data.get('tenant_code', ''),
        data.get('icao', ''),
    ])

    url_params = data.get('url_params')
    if isinstance(url_params, dict):
        candidates.extend([
            url_params.get('tenant_code', ''),
            url_params.get('icao', ''),
        ])


def _parse_form_data(value):
    if not value:
        return {}
    if isinstance(value, dict):
        return value
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
            if isinstance(parsed, dict):
                return parsed
        except Exception:
            return {}
    return {}


def tam_tenant_code(default: str = '') -> str:
    """
    Resolve tenant from request query params first, then Referer query params.
    This supports embedded chart API calls where chart/data requests may not
    directly carry tenant query params but the Referer URL does.
    """
    candidates = []
    try:
        args = getattr(request, 'args', {}) or {}
        candidates.extend([args.get('tenant_code', ''), args.get('icao', '')])
        _append_tenant_candidates(candidates, _parse_form_data(args.get('form_data', '')))

        ref = request.headers.get('Referer', '') or getattr(request, 'referrer', '') or ''
        if ref:
            q = parse_qs(urlparse(ref).query or '')
            candidates.extend([(q.get('tenant_code') or [''])[0], (q.get('icao') or [''])[0]])
            _append_tenant_candidates(candidates, _parse_form_data((q.get('form_data') or [''])[0]))

        payload = request.get_json(silent=True) or {}
        _append_tenant_candidates(candidates, payload)
        _append_tenant_candidates(candidates, _parse_form_data(payload.get('form_data', '')))
        query_context = _parse_form_data(payload.get('query_context', {}))
        _append_tenant_candidates(candidates, query_context)
        if isinstance(query_context, dict):
            _append_tenant_candidates(candidates, _parse_form_data(query_context.get('form_data', {})))
        for query in payload.get('queries', []) if isinstance(payload.get('queries'), list) else []:
            if isinstance(query, dict):
                _append_tenant_candidates(candidates, query.get('extras'))
    except Exception:
        pass

    for value in candidates:
        tenant = _sanitize_tenant(value)
        if tenant:
            return tenant

    return _sanitize_tenant(default)


JINJA_CONTEXT_ADDONS = {
    'tam_tenant_code': tam_tenant_code,
}
