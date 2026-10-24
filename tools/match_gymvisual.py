#!/usr/bin/env python3
"""Match existing numeric GIF files to GymVisual exercise names.

Reads only the public catalog HTML; does not download or redistribute media.
Matches by the GIF basename used in each product image URL, not by unrelated
GymVisual product IDs. Unmatched files are reported rather than guessed.
"""
import csv
import json
import re
import time
from pathlib import Path
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets/exercises"
OUT_JSON = ASSETS / "exercise_catalog.json"
OUT_CSV = ROOT / "gymvisual-match-report.csv"
BASE = "https://gymvisual.com/16-animated-gifs"
IMAGE_ID_RE = re.compile(r"/(\d{3,8})\.(?:gif|jpe?g|png)(?:$|[?])", re.I)
PRODUCT_ID_RE = re.compile(r"\[(\d{4,8})\]")


def clean_title(value):
    return " ".join((value or "").replace("\u00a0", " ").split()).strip(" -")


def image_file_id(url):
    path = urlparse(url).path
    match = IMAGE_ID_RE.search(path)
    return match.group(1) if match else None


def main():
    if not ASSETS.is_dir():
        raise SystemExit(f"Assets directory not found: {ASSETS}")

    session = requests.Session()
    session.headers["User-Agent"] = "Mozilla/5.0 (compatible; FitYarExerciseCatalog/1.0)"
    catalog = {}
    seen_pages = set()
    empty_pages = 0

    # GymVisual's category pagination may change; stop after several empty pages
    # instead of assuming a fixed number of pages.
    for page in range(1, 101):
        params = {"id_category": 16, "n": 100}
        if page > 1:
            params["p"] = page
        response = session.get(BASE, params=params, timeout=45)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        page_matches = 0

        cards = soup.select(".product-miniature, article.product-miniature, .js-product-miniature")
        if not cards:
            cards = soup.select("article, .product-container, .product")
        for card in cards:
            title_node = card.select_one(".product-title a, h2 a, h3 a, .product-title, h2, h3, a[title]")
            title = clean_title(
                (title_node.get("title") if title_node and title_node.has_attr("title") else "")
                or (title_node.get_text(" ", strip=True) if title_node else "")
            )
            if not title:
                continue

            urls = []
            for img in card.select("img"):
                for attr in ("data-full-size-image-url", "data-src", "src", "data-lazy-src"):
                    value = img.get(attr)
                    if value:
                        urls.append(urljoin(response.url, value))
                srcset = img.get("srcset", "")
                urls.extend(urljoin(response.url, part.strip().split(" ")[0])
                            for part in srcset.split(",") if part.strip())
            for url in urls:
                file_id = image_file_id(url)
                if file_id:
                    previous = catalog.get(file_id)
                    if previous is None or len(title) < len(previous):
                        catalog[file_id] = title
                    page_matches += 1

            # Retain product IDs only as a fallback when the actual media ID is
            # not exposed in the card. These IDs will match only if a GIF uses
            # the same numeric basename, and are never inferred from filenames.
            if not urls:
                match = PRODUCT_ID_RE.search(card.get_text(" ", strip=True))
                if match:
                    catalog.setdefault(match.group(1), title)

        if page_matches == 0:
            empty_pages += 1
        else:
            empty_pages = 0
        print(f"Page {page}: {len(catalog)} media IDs discovered")
        if page in seen_pages or empty_pages >= 4:
            break
        seen_pages.add(page)
        time.sleep(0.25)

    gifs = sorted(p for p in ASSETS.glob("*.gif") if p.stem.isdigit())
    entries, report = [], []
    for gif in gifs:
        name = catalog.get(gif.stem)
        if name:
            entries.append({
                "source": "GymVisual",
                "source_id": gif.stem,
                "name": name,
                "file": gif.name,
            })
            report.append([gif.name, gif.stem, name, "MATCH"])
        else:
            report.append([gif.name, "", "", "UNMATCHED"])

    entries.sort(key=lambda item: item["name"].casefold())
    OUT_JSON.write_text(json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with OUT_CSV.open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.writer(stream)
        writer.writerow(["gif_file", "media_id", "exercise_name", "status"])
        writer.writerows(report)

    print(f"Numeric GIFs: {len(gifs)}; matched: {len(entries)}; unmatched: {len(gifs) - len(entries)}")
    print(f"Generated {OUT_JSON.relative_to(ROOT)} and {OUT_CSV.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
