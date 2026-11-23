#!/usr/bin/env python3
"""Viipio's fast and release-gate checks; invoke Gradle only through its guard."""
from __future__ import annotations

import argparse
from pathlib import Path
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / "build/test-results/testDebugUnitTest"
TARGETED = (
    "tribixbite.cleverkeys.ViipioInputMethodServiceTest",
    "tribixbite.cleverkeys.ViipioInlinePersonalOfferTest",
    "tribixbite.cleverkeys.ViipioInlineWordGuidanceTest",
    "tribixbite.cleverkeys.ReleaseMetadataDriftTest",
    "tribixbite.cleverkeys.ViipioPersonalWordsTest",
    "tribixbite.cleverkeys.swipe.ctc.CtcLexiconMergeTest",
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


def report(expected: tuple[str, ...] = (), run_started_ns: int = 0) -> None:
    suites = {}
    stale = []
    for path in REPORTS.glob("TEST-*.xml"):
        if path.stat().st_mtime_ns < run_started_ns:
            stale.append(path.name)
        root = ET.parse(path).getroot()
        suites[root.attrib["name"]] = (
            int(root.attrib.get("tests", "0")),
            int(root.attrib.get("failures", "0")),
            int(root.attrib.get("errors", "0")),
            int(root.attrib.get("skipped", "0")),
        )
    missing = sorted(set(expected) - suites.keys())
    empty = sorted(name for name in expected if name in suites
                   and suites[name][0] <= suites[name][3])
    counts = [sum(row[i] for row in suites.values()) for i in range(4)]
    print(f"XML: {len(suites)} suites; tests={counts[0]}, failures={counts[1]}, "
          f"errors={counts[2]}, skipped={counts[3]}", flush=True)
    if not suites or counts[0] == 0 or missing or empty or stale or counts[1] or counts[2]:
        print(f"Verification failed; missing={missing}; empty/skipped={empty}; "
              f"old XML={stale}", file=sys.stderr)
        raise SystemExit(1)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("level", choices=("quick", "targeted", "full"))
    parser.add_argument("--rerun", action="store_true",
                        help="force a fresh guarded Gradle run after an UP-TO-DATE result")
    args = parser.parse_args()
    if args.rerun and args.level == "quick":
        parser.error("--rerun applies only to targeted or full")
    run("git", "diff", "--check")
    run("git", "diff", "--cached", "--check")
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
    if args.rerun:
        cmd += ["--rerun-tasks"]
    run_started_ns = time.time_ns()
    run(*cmd)
    report(TARGETED if args.level == "targeted" else (), run_started_ns)


if __name__ == "__main__":
    main()
