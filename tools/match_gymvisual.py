#!/usr/bin/env python3
"""Create a searchable offline catalog for numeric GIFs already in FitYar.

Scrapes exercise titles/IDs only. It does not download, copy, or redistribute
GymVisual media. Exact-ID matching only; unmatched files are never guessed.
"""
import csv, json, re, time
from pathlib import Path
import requests
from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/exercises"
OUT_JSON = ASSETS / "exercise_catalog.json"
OUT_CSV = ROOT / "gymvisual-match-report.csv"
BASE = "https://gymvisual.com/16-animated-gifs"
ID_RE = re.compile(r"\[(\d{4,8})\]")

def main():
    if not ASSETS.is_dir():
        raise SystemExit(f"Assets directory not found: {ASSETS}")
    session = requests.Session()
    session.headers["User-Agent"] = "Mozilla/5.0 (compatible; FitYarExerciseCatalog/1.0)"
    catalog = {}
    for page in range(1, 67):
        params = {"id_category": 16, "n": 100}
        if page > 1:
            params["p"] = page
        response = session.get(BASE, params=params, timeout=45)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        for node in soup.select("h2, h3, h4, .product-title, .product-miniature, article"):
            text = " ".join(node.get_text(" ", strip=True).split())
            match = ID_RE.search(text)
            if not match:
                continue
            exercise_id = match.group(1)
            title = " ".join(text[:match.start()].replace("\u00a0", " ").split()).strip(" -")
            if title and len(title) <= 180:
                if exercise_id not in catalog or len(title) < len(catalog[exercise_id]):
                    catalog[exercise_id] = title
        print(f"Page {page}/66: {len(catalog)} IDs")
        time.sleep(0.2)

    gifs = sorted(p for p in ASSETS.glob("*.gif") if p.stem.isdigit())
    entries, report = [], []
    for gif in gifs:
        name = catalog.get(gif.stem)
        if name:
            entries.append({"source":"GymVisual","source_id":gif.stem,"name":name,"file":gif.name})
            report.append([gif.name, gif.stem, name, "MATCH"])
        else:
            report.append([gif.name, "", "", "UNMATCHED"])
    entries.sort(key=lambda x: x["name"].casefold())
    OUT_JSON.write_text(json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with OUT_CSV.open("w", newline="", encoding="utf-8-sig") as f:
        writer = csv.writer(f)
        writer.writerow(["gif_file","gymvisual_id","exercise_name","status"])
        writer.writerows(report)
    print(f"Numeric GIFs: {len(gifs)}; exact matches: {len(entries)}; unmatched: {len(gifs)-len(entries)}")
    print(f"Generated {OUT_JSON.relative_to(ROOT)} and {OUT_CSV.relative_to(ROOT)}")
    print("Name-only catalog generated; no unverified exercise details are added.")

if __name__ == "__main__":
    main()
