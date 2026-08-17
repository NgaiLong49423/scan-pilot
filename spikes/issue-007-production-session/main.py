"""Temporary same-origin session-cookie spike for Eligibility Spike Issue #7.

This service is not Scan Pilot's production API. It uses no GitHub App,
credentials, database, repository data, or Gemini calls. Its only purpose is
to prove that a browser can receive an HttpOnly cookie and send it back when
the test page and endpoint share one deployed Cloud Run origin.
"""

import json
import os
import secrets
from http import HTTPStatus
from http.cookies import SimpleCookie
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


COOKIE_NAME = "__Host-scan-pilot-session-spike"
COOKIE_MAX_AGE_SECONDS = 600


class SpikeHandler(BaseHTTPRequestHandler):
    """Serve a minimal same-origin browser-cookie verification flow."""

    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path == "/":
            self._send_html(HTTPStatus.OK, PAGE)
        elif path == "/spike/session/start":
            self._start_session()
        elif path == "/spike/session/check":
            self._check_session()
        else:
            self._send_json(HTTPStatus.NOT_FOUND, {"error": "not_found"})

    def log_message(self, _format, *_args):
        """Do not retain request logs; this spike has no operational value."""

    def _start_session(self):
        session_id = secrets.token_urlsafe(32)
        body = {
            "status": "started",
            "purpose": "production-same-origin-session-spike",
            "next": "/spike/session/check",
        }
        self.send_response(HTTPStatus.OK)
        self._send_common_headers()
        self.send_header(
            "Set-Cookie",
            f"{COOKIE_NAME}={session_id}; Path=/; Max-Age={COOKIE_MAX_AGE_SECONDS}; "
            "Secure; HttpOnly; SameSite=Lax",
        )
        self._write_json_body(body)

    def _check_session(self):
        cookie = SimpleCookie(self.headers.get("Cookie"))
        received = COOKIE_NAME in cookie and bool(cookie[COOKIE_NAME].value)
        self._send_json(
            HTTPStatus.OK,
            {
                "status": "ok",
                "cookieReceived": received,
                "purpose": "production-same-origin-session-spike",
            },
        )

    def _send_json(self, status, payload):
        self.send_response(status)
        self._send_common_headers()
        self._write_json_body(payload)

    def _write_json_body(self, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_html(self, status, body):
        encoded = body.encode("utf-8")
        self.send_response(status)
        self._send_common_headers()
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def _send_common_headers(self):
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")


PAGE = """<!doctype html>
<html lang="en">
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Scan Pilot Session Spike</title>
  <body>
    <h1>Scan Pilot production session spike</h1>
    <p>This temporary page tests an HttpOnly cookie on this same Cloud Run origin.</p>
    <button id="run">Run verification</button>
    <pre id="result" aria-live="polite">Not started</pre>
    <script>
      document.getElementById('run').addEventListener('click', async () => {
        const result = document.getElementById('result');
        result.textContent = 'Running...';
        try {
          await fetch('/spike/session/start', {
            credentials: 'same-origin',
            cache: 'no-store'
          });
          const response = await fetch('/spike/session/check', {
            credentials: 'same-origin',
            cache: 'no-store'
          });
          const data = await response.json();
          result.textContent = data.cookieReceived
            ? 'PASS: server received the HttpOnly session cookie.'
            : 'FAIL: server did not receive the session cookie.';
        } catch (error) {
          result.textContent = `ERROR: ${error.message}`;
        }
      });
    </script>
  </body>
</html>"""


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "8080"))
    ThreadingHTTPServer(("0.0.0.0", port), SpikeHandler).serve_forever()
