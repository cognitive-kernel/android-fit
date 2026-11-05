#!/usr/bin/env python3
"""Synchronize exercise gender and filenames from GymVisual's official exercise list.

The script uses GymVisual's published reference ID + Gender columns. It never
infers gender from an exercise name or image. Unmatched rows remain unchanged.
Run from the repository root. Default is a dry run; pass --apply to rename files.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

SHEET_URL = (
    "https://docs.google.com/spreadsheets/d/"
    "1dUMCUSk8rqPoLZ5Xltg7shqgfqyHfALq/export?format=xlsx"
)
NS = {
    "a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
}
CATALOG = Path("app/src/main/assets/exercise_catalog.json")
ASSET_DIR = Path("app/src/main/assets/exercises")
GENDERS = {"male": "male", "female": "female"}


def cell_value(cell, shared):
    value = cell.find("a:v", NS)
    if value is None:
        return ""
    raw = value.text or ""
    if cell.get("t") == "s":
        return shared[int(raw)]
    return raw


def read_reference_table(xlsx: Path):
    with zipfile.ZipFile(xlsx) as archive:
        shared = []
        if "xl/sharedStrings.xml" in archive.namelist():
            root = ET.fromstring(archive.read("xl/sharedStrings.xml"))
            for item in root.findall("a:si", NS):
                shared.append("".join(node.text or "" for node in item.findall(".//a:t", NS)))
        sheet = ET.fromstring(archive.read("xl/worksheets/sheet1.xml"))
        rows = sheet.findall(".//a:sheetData/a:row", NS)
        if not rows:
            raise RuntimeError("The official workbook has no worksheet rows.")
        table = {}
        headers = {}
        for cell in rows[0].findall("a:c", NS):
            col = re.match(r"[A-Z]+", cell.get("r", "")).group(0)
            headers[col] = cell_value(cell, shared).strip().lower()
        id_col = next((c for c, h in headers.items() if h == "id"), None)
        gender_col = next((c for c, h in headers.items() if h == "gender"), None)
        if not id_col or not gender_col:
            raise RuntimeError("Official workbook is missing ID or Gender columns.")
        for row in rows[1:]:
            values = {re.match(r"[A-Z]+", c.get("r", "")).group(0): cell_value(c, shared).strip()
                      for c in row.findall("a:c", NS)}
            ref_id = values.get(id_col, "").lstrip("0") or "0"
            gender = values.get(gender_col, "").strip().lower()
            if ref_id and gender in GENDERS:
                if ref_id in table and table[ref_id] != gender:
                    raise RuntimeError(f"Conflicting gender values for GymVisual ID {ref_id}.")
                table[ref_id] = gender
        return table


def safe_stem(name):
    stem = re.sub(r"\s*\((?:male|female)\)\s*$", "", name, flags=re.I)
    stem = re.sub(r"[^A-Za-z0-9]+", "_", stem).strip("_")
    return stem or "exercise"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true", help="Apply file and catalog changes.")
    parser.add_argument("--workbook", type=Path, help="Use a previously downloaded official XLSX.")
    args = parser.parse_args()

    if not CATALOG.is_file() or not ASSET_DIR.is_dir():
        raise SystemExit("Run this script from the repository root; catalog/assets not found.")

    workbook = args.workbook or Path(".cache/gymvisual-exercises.xlsx")
    if not workbook.is_file():
        workbook.parent.mkdir(parents=True, exist_ok=True)
        print("Downloading GymVisual's published exercise reference workbook...")
        urllib.request.urlretrieve(SHEET_URL, workbook)

    reference = read_reference_table(workbook)
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    rows = catalog.get("exercises", [])
    planned = []
    unmatched = 0

    for row in rows:
        asset_id = str(row["id"])
        if len(asset_id) < 3 or not asset_id[:-2].isdigit():
            unmatched += 1
            continue
        ref_id = asset_id[:-2].lstrip("0") or "0"
        gender = reference.get(ref_id)
        if not gender:
            unmatched += 1
            continue
        old_file = str(row["file"])
        old_path = ASSET_DIR / old_file
        if not old_path.is_file():
            raise RuntimeError(f"Catalog asset is missing: {old_path}")
        new_name = f"{safe_stem(row['name'])} ({gender})_[{asset_id}].gif"
        new_path = ASSET_DIR / new_name
        if new_path.exists() and new_path.resolve() != old_path.resolve():
            raise RuntimeError(f"Filename collision: {new_path}")
        new_display_name = re.sub(r"\s*\((?:male|female)\)\s*$", "", row["name"], flags=re.I)
        new_display_name = f"{new_display_name} ({gender})"
        if row.get("gender") != gender or row.get("name") != new_display_name or old_file != new_name:
            planned.append((row, old_path, new_path, gender, new_display_name))

    print(f"Catalog rows: {len(rows)}")
    print(f"Official GymVisual IDs matched: {len(planned)} rows requiring update")
    print(f"Unmatched rows retained as-is: {unmatched}")
    for row, old_path, new_path, gender, display_name in planned[:30]:
        print(f"{row['id']}: {old_path.name} -> {new_path.name} [{gender}]")
    if len(planned) > 30:
        print(f"... and {len(planned) - 30} more")

    if not args.apply:
        print("Dry run only. Re-run with --apply to rename files and update catalog.")
        return

    # Preflight every target before changing any filename.
    for _, old_path, new_path, _, _ in planned:
        if not old_path.is_file():
            raise RuntimeError(f"Source disappeared: {old_path}")
        if new_path.exists() and new_path.resolve() != old_path.resolve():
            raise RuntimeError(f"Target already exists: {new_path}")

    completed = []
    try:
        for row, old_path, new_path, gender, display_name in planned:
            if old_path != new_path:
                old_path.rename(new_path)
                completed.append((old_path, new_path))
            row["gender"] = gender
            row["name"] = display_name
            row["file"] = new_path.name
        catalog["version"] = int(catalog.get("version", 1)) + 1
        catalog["count"] = len(rows)
        temp = CATALOG.with_suffix(".json.tmp")
        temp.write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")) + "\n",
                        encoding="utf-8")
        temp.replace(CATALOG)
    except Exception:
        for old_path, new_path in reversed(completed):
            if new_path.exists() and not old_path.exists():
                new_path.rename(old_path)
        raise

    print(f"Applied {len(planned)} updates. Unmatched rows were not changed.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
