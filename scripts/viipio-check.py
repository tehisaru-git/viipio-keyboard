#!/usr/bin/env python3
"""Viipio's fast and release-gate checks; invoke Gradle only through its guard."""
from __future__ import annotations

import argparse
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / "build/test-results/testDebugUnitTest"
TARGETED = (
    "tribixbite.cleverkeys.EstonianSwipeServiceTest",
    "tribixbite.cleverkeys.estonianui.KeyboardSurfaceTest",
    "tribixbite.cleverkeys.estonianui.KeyboardSurfaceViewTest",
    "tribixbite.cleverkeys.ViipioTypedWordTrackerTest",
    "tribixbite.cleverkeys.ViipioTypedWordCaptureTest",
    "tribixbite.cleverkeys.ViipioCorrectionEvidenceTest",
    "tribixbite.cleverkeys.ViipioInputEvidenceTest",
    "tribixbite.cleverkeys.swipeui.ManualCompletionsTest",
    "tribixbite.cleverkeys.estoniantelemetry.TelemetryControllerTest",
    "tribixbite.cleverkeys.estoniantelemetry.TelemetryActivityTest",
    "tribixbite.cleverkeys.estoniantelemetry.InputEventsTest",
    "tribixbite.cleverkeys.estoniantelemetry.InputFlushScheduleTest",
    "tribixbite.cleverkeys.estonianstudy.SwipeStudySessionTest",
)


def run(*cmd: str) -> None:
    print("+", " ".join(cmd), flush=True)
    result = subprocess.run(cmd, cwd=ROOT, check=False)
    if result.returncode:
        raise SystemExit(result.returncode)


def report(expected: tuple[str, ...] = ()) -> None:
    suites = {}
    for path in REPORTS.glob("TEST-*.xml"):
        root = ET.parse(path).getroot()
        suites[root.attrib["name"]] = (
            int(root.attrib.get("tests", "0")),
            int(root.attrib.get("failures", "0")),
            int(root.attrib.get("errors", "0")),
            int(root.attrib.get("skipped", "0")),
        )
    missing = sorted(set(expected) - suites.keys())
    counts = [sum(row[i] for row in suites.values()) for i in range(4)]
    print(f"XML: {len(suites)} suites; tests={counts[0]}, failures={counts[1]}, "
          f"errors={counts[2]}, skipped={counts[3]}", flush=True)
    if not suites or counts[0] == 0 or missing or counts[1] or counts[2]:
        print(f"Verification failed; missing suites: {missing}", file=sys.stderr)
        raise SystemExit(1)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("level", choices=("quick", "targeted", "full"))
    args = parser.parse_args()
    run("git", "diff", "--check")
    run(sys.executable, "-m", "unittest", "discover", "-s", "src/test/python",
        "-p", "test_viipio_*.py", "-q")
    if args.level == "quick":
        return
    cmd = ["bash", "scripts/gradle-guard.sh", "testDebugUnitTest", "--offline"]
    if args.level == "targeted":
        for suite in TARGETED:
            cmd += ["--tests", suite]
    else:
        cmd += ["-PviipioTestForkEvery=40"]
    run(*cmd)
    report(TARGETED if args.level == "targeted" else ())


if __name__ == "__main__":
    main()
