"""Temporary Cloud Run endpoint for Eligibility Spike Issue #6.

This is deliberately not part of the Scan Pilot product API. It accepts no
credentials, accesses no project data, and exists only to verify browser CORS
from the Google AI Studio origin.
"""

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


ALLOWED_ORIGIN = "https://aistudio.google.com"


class SpikeHandler(BaseHTTPRequestHandler):
    """Serve only the public, credential-free CORS verification endpoint."""

    def do_OPTIONS(self):
        if not self._is_allowed_browser_origin():
            self._send_json(403, {"error": "origin_not_allowed"})
            return

        self.send_response(204)
        self._send_cors_headers()
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Access-Control-Max-Age", "600")
        self.end_headers()

    def do_GET(self):
        if self.path != "/spike/ping":
            self._send_json(404, {"error": "not_found"})
            return

        if not self._is_allowed_browser_origin():
            self._send_json(403, {"error": "origin_not_allowed"})
            return

        self._send_json(
            200,
            {
                "status": "ok",
                "purpose": "ai-studio-cloud-run-cors-spike",
            },
        )

    def log_message(self, _format, *_args):
        """Avoid request logging because the spike needs no retained request data."""

    def _is_allowed_browser_origin(self):
        """Permit the AI Studio browser origin, plus origin-less health checks."""
        origin = self.headers.get("Origin")
        return origin is None or origin == ALLOWED_ORIGIN

    def _send_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", ALLOWED_ORIGIN)
        self.send_header("Vary", "Origin")

    def _send_json(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        if self.headers.get("Origin") == ALLOWED_ORIGIN:
            self._send_cors_headers()
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "8080"))
    server = ThreadingHTTPServer(("0.0.0.0", port), SpikeHandler)
    server.serve_forever()
