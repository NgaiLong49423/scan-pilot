"""Temporary GitHub OAuth callback spike for Eligibility Spike Issue #7.

This service validates a same-origin browser session pattern and the GitHub
authorization-code handoff. It is deliberately not the Scan Pilot production
authentication implementation: it stores no user, repository, access token,
or refresh token after the one-time exchange verification.
"""

import base64
import hashlib
import hmac
import json
import os
import secrets
import urllib.parse
import urllib.request
from http import HTTPStatus
from http.cookies import SimpleCookie
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


OAUTH_COOKIE_NAME = "__Host-scan-pilot-oauth-spike"
OAUTH_COOKIE_MAX_AGE_SECONDS = 600
GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token"


class OAuthSpikeHandler(BaseHTTPRequestHandler):
    """Serve a minimal, non-persistent OAuth callback verification flow."""

    def do_GET(self):
        parsed = urllib.parse.urlsplit(self.path)
        if parsed.path == "/":
            self._send_html(HTTPStatus.OK, PAGE)
        elif parsed.path == "/auth/github/start":
            self._start_github_authorization()
        elif parsed.path == "/auth/github/callback":
            self._handle_github_callback(parsed.query)
        elif parsed.path == "/healthz":
            self._send_json(HTTPStatus.OK, {"status": "ok"})
        else:
            self._send_json(HTTPStatus.NOT_FOUND, {"error": "not_found"})

    def log_message(self, _format, *_args):
        """Never write request paths or query strings from this OAuth spike."""

    def _start_github_authorization(self):
        client_id = os.environ.get("GITHUB_CLIENT_ID")
        public_base_url = os.environ.get("PUBLIC_BASE_URL")
        if not client_id or not public_base_url:
            self._send_html(
                HTTPStatus.SERVICE_UNAVAILABLE,
                configuration_needed_page(),
            )
            return

        state = secrets.token_urlsafe(32)
        verifier = secrets.token_urlsafe(64)
        challenge = base64.urlsafe_b64encode(
            hashlib.sha256(verifier.encode("ascii")).digest()
        ).rstrip(b"=").decode("ascii")
        cookie_value = encode_cookie_payload(state, verifier)
        callback_url = f"{public_base_url.rstrip('/')}/auth/github/callback"
        query = urllib.parse.urlencode(
            {
                "client_id": client_id,
                "redirect_uri": callback_url,
                "state": state,
                "code_challenge": challenge,
                "code_challenge_method": "S256",
            }
        )
        self.send_response(HTTPStatus.FOUND)
        self._send_common_headers()
        self.send_header(
            "Set-Cookie",
            f"{OAUTH_COOKIE_NAME}={cookie_value}; Path=/; "
            f"Max-Age={OAUTH_COOKIE_MAX_AGE_SECONDS}; Secure; HttpOnly; SameSite=Lax",
        )
        self.send_header("Location", f"{GITHUB_AUTHORIZE_URL}?{query}")
        self.end_headers()

    def _handle_github_callback(self, raw_query):
        query = urllib.parse.parse_qs(raw_query, keep_blank_values=True)
        state = single_query_value(query, "state")
        code = single_query_value(query, "code")
        cookie_payload = decode_cookie_payload(
            SimpleCookie(self.headers.get("Cookie")).get(OAUTH_COOKIE_NAME)
        )
        self._clear_oauth_cookie()

        if single_query_value(query, "error"):
            self._send_html(HTTPStatus.BAD_REQUEST, result_page("GitHub authorization was cancelled or denied."))
            return
        if not state or not code or not cookie_payload:
            self._send_html(HTTPStatus.BAD_REQUEST, result_page("OAuth callback validation failed."))
            return

        expected_state, verifier = cookie_payload
        if not hmac.compare_digest(state, expected_state):
            self._send_html(HTTPStatus.BAD_REQUEST, result_page("OAuth callback validation failed."))
            return

        client_id = os.environ.get("GITHUB_CLIENT_ID")
        client_secret = os.environ.get("GITHUB_CLIENT_SECRET")
        if not client_id or not client_secret:
            self._send_html(
                HTTPStatus.SERVICE_UNAVAILABLE,
                result_page("Callback validation passed, but the temporary service is not fully configured."),
            )
            return

        if not exchange_code_for_token(client_id, client_secret, code, verifier):
            self._send_html(HTTPStatus.BAD_GATEWAY, result_page("GitHub token exchange could not be verified."))
            return

        self._send_html(
            HTTPStatus.OK,
            result_page("PASS: GitHub authorization-code exchange succeeded. No token was stored or displayed."),
        )

    def _clear_oauth_cookie(self):
        self._pending_clear_cookie = True

    def _send_json(self, status, payload):
        self.send_response(status)
        self._send_common_headers()
        self._write_json_body(payload)

    def _send_html(self, status, body):
        encoded = body.encode("utf-8")
        self.send_response(status)
        self._send_common_headers()
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def _write_json_body(self, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_common_headers(self):
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")
        if getattr(self, "_pending_clear_cookie", False):
            self.send_header(
                "Set-Cookie",
                f"{OAUTH_COOKIE_NAME}=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Lax",
            )


def encode_cookie_payload(state, verifier):
    """Encode transient browser-held CSRF/PKCE data without logging it."""
    payload = json.dumps({"state": state, "verifier": verifier}).encode("utf-8")
    return base64.urlsafe_b64encode(payload).rstrip(b"=").decode("ascii")


def decode_cookie_payload(cookie):
    if not cookie:
        return None
    try:
        padded = cookie.value + "=" * (-len(cookie.value) % 4)
        payload = json.loads(base64.urlsafe_b64decode(padded.encode("ascii")))
        state = payload["state"]
        verifier = payload["verifier"]
        if not isinstance(state, str) or not isinstance(verifier, str):
            return None
        return state, verifier
    except (KeyError, TypeError, ValueError):
        return None


def single_query_value(query, name):
    values = query.get(name)
    return values[0] if values and len(values) == 1 and values[0] else None


def exchange_code_for_token(client_id, client_secret, code, verifier):
    """Exchange once; never persist, display, or log the returned token."""
    body = urllib.parse.urlencode(
        {
            "client_id": client_id,
            "client_secret": client_secret,
            "code": code,
            "code_verifier": verifier,
        }
    ).encode("ascii")
    request = urllib.request.Request(
        GITHUB_TOKEN_URL,
        data=body,
        headers={"Accept": "application/json", "Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except Exception:  # Do not expose provider details or credentials in this spike.
        return False
    return bool(payload.get("access_token")) and not payload.get("error")


def result_page(message):
    return f"""<!doctype html>
<html lang=\"en\"><meta charset=\"utf-8\"><title>Scan Pilot OAuth Spike</title>
<body><h1>Scan Pilot OAuth spike</h1><p>{message}</p></body></html>"""


def configuration_needed_page():
    return result_page("Temporary OAuth configuration is not ready yet.")


PAGE = """<!doctype html>
<html lang=\"en\"><meta charset=\"utf-8\"><title>Scan Pilot OAuth Spike</title>
<body>
  <h1>Scan Pilot GitHub OAuth spike</h1>
  <p>Temporary Eligibility Spike only. It stores no GitHub token or user data.</p>
  <p><a href=\"/auth/github/start\">Start GitHub authorization test</a></p>
</body>
</html>"""


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "8080"))
    ThreadingHTTPServer(("0.0.0.0", port), OAuthSpikeHandler).serve_forever()
