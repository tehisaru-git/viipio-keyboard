#!/usr/bin/env python3
"""Wake hourly Viipio work until the published v0.6.0 APK exists."""
import datetime
import json
import subprocess

REPO = "tehisaru-git/viipio-keyboard"
APK = "ViipioKeyboard-v0.6.0-arm64-v8a.apk"

try:
    result = subprocess.run(
        ["gh", "release", "view", "v0.6.0", "-R", REPO,
         "--json", "tagName,isDraft,assets"],
        capture_output=True, text=True, timeout=20, check=False,
    )
    if result.returncode == 0:
        release = json.loads(result.stdout)
        if (release.get("tagName") == "v0.6.0" and
                release.get("isDraft") is False and
                any(asset.get("name") == APK for asset in release.get("assets", []))):
            print("released")
            raise SystemExit(0)
except (OSError, ValueError, subprocess.TimeoutExpired):
    pass  # An uncertain network result must wake the agent for a direct check.

# Deliberately changes once per hour while unreleased, then stays constant forever.
# The cron change-detector thus runs the agent hourly only until publication.
print("unreleased:" + datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H"))
