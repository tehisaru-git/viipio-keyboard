#!/usr/bin/env python3
"""Build a *reference* ET word-form list, not the shipping keyboard ranking.

Source: Indrek Hein (2022), Eesti keele ühendkorpuse 2021 lemmade ja
sõnavormide sagedusloendid, Eesti Keele Instituut, CC BY.
DOI: 10.15155/3-00-0000-0000-0000-08D1FL
https://metashare.ut.ee/repository/browse/eesti-keele-uhendkorpuse-2021-lemmade-ja-sonavormide-sagedusloendid/4547c7bfa0d411eebb4773db10791bcf142faa2513f94e4fabe1a0f710c1e3fa/

This list is web/news-heavy, excludes capitalized forms and is NOT a keyboard
quality benchmark. Do not replace the bundled dictionary with it without
held-out swipe tests and a review of licensing and source files.
"""
import argparse
import csv
import os
import sqlite3
import sys
import tempfile
from pathlib import Path
from urllib.request import urlopen

SOURCE = "https://arhiiv.eki.ee/tarkvara/enc21/21.vormid.alp.txt"
ALPHABET = frozenset("abcdefghijklmnopqrstuvwxyzõäöüšž")


def top_forms(lines, database: sqlite3.Connection, limit: int = 1000) -> list[tuple[str, int]]:
    database.execute("CREATE TABLE forms (word TEXT PRIMARY KEY, occurrences INTEGER NOT NULL)")
    pending: list[tuple[str, int]] = []
    statement = ("INSERT INTO forms(word, occurrences) VALUES(?, ?) "
                 "ON CONFLICT(word) DO UPDATE SET occurrences=occurrences+excluded.occurrences")
    for line in lines:
        if not line.startswith("\t"):
            continue
        fields = line.rstrip("\r\n").split("\t")
        if len(fields) != 4:
            continue
        _, _tag, word, number = fields
        if not (2 <= len(word) <= 48 and all(ch in ALPHABET for ch in word)):
            continue
        try:
            count = int(number)
        except ValueError:
            continue
        if count < 1:
            continue
        pending.append((word, count))
        if len(pending) >= 5000:
            database.executemany(statement, pending)
            pending.clear()
    if pending:
        database.executemany(statement, pending)
    return [(str(word), int(count)) for word, count in database.execute(
        "SELECT word, occurrences FROM forms ORDER BY occurrences DESC, word ASC LIMIT ?", (limit,))]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, help="Already downloaded EKI source; otherwise stream the official URL")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--limit", type=int, default=1000)
    args = parser.parse_args()
    if not 1 <= args.limit <= 10000:
        parser.error("--limit must be 1..10000")
    handle = tempfile.NamedTemporaryFile(prefix="viipio-eki-", suffix=".sqlite", dir=os.environ.get("TMPDIR"), delete=False)
    db_path = Path(handle.name)
    handle.close()
    try:
        with sqlite3.connect(db_path) as database:
            database.execute("PRAGMA journal_mode=OFF")
            database.execute("PRAGMA synchronous=OFF")
            if args.input is None:
                with urlopen(SOURCE, timeout=120) as stream:
                    rows = top_forms((line.decode("utf-8") for line in stream), database, args.limit)
            else:
                with args.input.open(encoding="utf-8") as stream:
                    rows = top_forms(stream, database, args.limit)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        with args.output.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, lineterminator="\n")
            writer.writerow(("word_form", "occurrences_in_selected_enc21_subcorpora"))
            writer.writerows(rows)
        print(f"Generated {len(rows)} public corpus word forms: {args.output}")
    finally:
        db_path.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
