"""Serve deterministic subscription responses for Veyr device regression tests."""

import argparse
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from urllib.parse import urlsplit


VLESS = "vless://00000000-0000-4000-8000-000000000001@192.0.2.10:443?encryption=none"
HYSTERIA2 = "hysteria2://fixture-password@192.0.2.20:443"
SCENARIOS = {
    "baseline": (200, f"{VLESS}#Fixture-VLESS\n{HYSTERIA2}#Fixture-Hysteria2\n"),
    "rename": (200, f"{VLESS}#Fixture-VLESS-Renamed\n{HYSTERIA2}#Fixture-Hysteria2\n"),
    "remove": (200, f"{HYSTERIA2}#Fixture-Hysteria2\n"),
    "unavailable": (503, "Fixture service unavailable\n"),
    "invalid": (200, "This response contains no subscription profiles.\n"),
}


def handler_for(scenario_file: Path):
    class SubscriptionHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            if urlsplit(self.path).path != "/subscription":
                self.send_error(404)
                return

            try:
                scenario = scenario_file.read_text(encoding="utf-8-sig").strip()
                status, text = SCENARIOS[scenario]
            except (OSError, UnicodeError, KeyError):
                self.send_error(500, "Invalid fixture scenario")
                return

            body = text.encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)

    return SubscriptionHandler


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scenario-file", type=Path, required=True)
    parser.add_argument("--bind", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()

    with HTTPServer((args.bind, args.port), handler_for(args.scenario_file)) as server:
        print(f"Serving http://{args.bind}:{args.port}/subscription", flush=True)
        server.serve_forever()


if __name__ == "__main__":
    main()
