#!/usr/bin/env python3
"""
Regenerate app/src/main/assets/json/game_words.json from HSK vocab CSV files.

Data source:
  https://github.com/plaktos/hsk_csv
  - hsk1.csv ... hsk6.csv
  - 3 columns per row: word, pinyin, english

Rules:
  - EASY:   HSK1-2, allow 1-char or 2-char words
  - MEDIUM: HSK3-4, only 2-char words
  - HARD:   HSK5-6, only 2-char words
  - keep only full-Chinese words (no punctuation/Latin chars)
  - hint must be a short English gloss
  - pinyin must be generated using project PinyinUtils.wordToPinyin()
"""

from __future__ import annotations

import csv
import json
import os
import re
import stat
import shutil
import subprocess
import sys
import tempfile
import urllib.request
from collections import OrderedDict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TMP_DIR = ROOT / "tools" / "_tmp_hsk_csv"
OUT_JSON = ROOT / "app" / "src" / "main" / "assets" / "json" / "game_words.json"
PINYIN_UTILS_JAVA = ROOT / "app" / "src" / "main" / "java" / "com" / "example" / "chinese_game" / "utils" / "PinyinUtils.java"

REPO_URL = "https://github.com/plaktos/hsk_csv.git"
TARGET_PER_LEVEL = 130
VALID_HINT_MIN_LEN = 2
ZH_WORD_RE = re.compile(r"^[\u4e00-\u9fff]+$")


def run(cmd: list[str], cwd: Path | None = None) -> None:
    subprocess.run(cmd, check=True, cwd=str(cwd) if cwd else None)


def clone_source_repo() -> Path:
    def _on_rm_error(func, path, _exc_info):
        os.chmod(path, stat.S_IWRITE)
        func(path)

    if TMP_DIR.exists():
        shutil.rmtree(TMP_DIR, onexc=_on_rm_error)
    run(["git", "clone", "--depth", "1", REPO_URL, str(TMP_DIR)], cwd=ROOT)
    return TMP_DIR


def simplify_hint(english: str) -> str:
    """
    Convert dictionary-like gloss to a short in-game hint.
    """
    s = (english or "").strip()
    if not s:
        return "A common Chinese word."

    # Remove parenthetical details and keep first gloss fragment.
    s = re.sub(r"\([^)]*\)", "", s)
    s = re.sub(r"\[[^\]]*\]", "", s)
    parts = [p.strip(" .,;/") for p in re.split(r"[,;/]", s) if p.strip(" .,;/")]
    s = parts[0] if parts else ""
    if not s:
        return "A common Chinese word."

    # Keep short, readable hints.
    s = re.sub(r"\s+", " ", s).strip()
    if re.search(r"[\u4e00-\u9fff]", s):
        return "A common Chinese word."
    if len(s) < VALID_HINT_MIN_LEN:
        return "A common Chinese word."
    if len(s) > 72:
        s = s[:69].rstrip() + "..."
    s = s[0].upper() + s[1:]
    if not s.endswith("."):
        s += "."
    return s


def read_hsk_rows(repo_dir: Path, level: int) -> list[dict[str, str]]:
    p = repo_dir / f"hsk{level}.csv"
    rows: list[dict[str, str]] = []
    with p.open("r", encoding="utf-8", newline="") as f:
        reader = csv.reader(f)
        for row in reader:
            if len(row) < 3:
                continue
            word = row[0].strip()
            eng = row[2].strip()
            rows.append({"word": word, "hint": simplify_hint(eng)})
    return rows


def pick_words(
    levels: list[int],
    allow_lengths: set[int],
    repo_dir: Path,
    seen_global: set[str],
) -> list[dict[str, str]]:
    out: list[dict[str, str]] = []
    for lv in levels:
        for row in read_hsk_rows(repo_dir, lv):
            w = row["word"]
            if w in seen_global:
                continue
            if not ZH_WORD_RE.fullmatch(w):
                continue
            if len(w) not in allow_lengths:
                continue
            out.append({"word": w, "pos_tag": "NN", "hint": row["hint"]})
            seen_global.add(w)
            if len(out) >= TARGET_PER_LEVEL:
                return out
    return out


def find_pinyin4j_jar() -> Path:
    gradle_cache = Path.home() / ".gradle" / "caches" / "modules-2" / "files-2.1" / "com.belerweb" / "pinyin4j" / "2.5.0"
    for root, _, files in os.walk(gradle_cache):
        if "pinyin4j-2.5.0.jar" in files:
            return Path(root) / "pinyin4j-2.5.0.jar"
    raise FileNotFoundError("Cannot find pinyin4j-2.5.0.jar in Gradle cache.")


def generate_pinyin_by_project_code(words: list[str]) -> list[str]:
    """
    Compile and run a tiny Java bridge that calls PinyinUtils.wordToPinyin().
    """
    pinyin_jar = find_pinyin4j_jar()
    with tempfile.TemporaryDirectory() as td:
        td_path = Path(td)
        bridge_java = td_path / "PinyinBridge.java"
        classes = td_path / "classes"
        classes.mkdir(parents=True, exist_ok=True)
        bridge_java.write_text(
            """
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.example.chinese_game.utils.PinyinUtils;

public class PinyinBridge {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) {
            bw.write(PinyinUtils.wordToPinyin(line));
            bw.newLine();
        }
        bw.flush();
    }
}
            """.strip()
            + "\n",
            encoding="utf-8",
        )

        run(
            [
                "javac",
                "-encoding",
                "UTF-8",
                "-cp",
                str(pinyin_jar),
                "-d",
                str(classes),
                str(PINYIN_UTILS_JAVA),
                str(bridge_java),
            ],
            cwd=ROOT,
        )

        proc = subprocess.run(
            [
                "java",
                "-cp",
                str(classes) + os.pathsep + str(pinyin_jar),
                "PinyinBridge",
            ],
            input="\n".join(words),
            text=True,
            capture_output=True,
            encoding="utf-8",
            check=True,
            cwd=str(ROOT),
        )
        pinyins = [line.strip() for line in proc.stdout.splitlines()]
        if len(pinyins) != len(words):
            raise RuntimeError(f"Pinyin count mismatch: got {len(pinyins)} expected {len(words)}")
        return pinyins


def verify(entries: list[dict[str, str]]) -> None:
    counts = {"EASY": 0, "MEDIUM": 0, "HARD": 0}
    seen = set()
    for idx, e in enumerate(entries, start=1):
        w = e["word"]
        d = e["difficulty"]
        h = e["hint"]
        p = e["pinyin"]
        if not ZH_WORD_RE.fullmatch(w):
            raise ValueError(f"Non-Chinese word at #{idx}: {w!r}")
        if d == "EASY":
            if len(w) not in (1, 2):
                raise ValueError(f"EASY length invalid at #{idx}: {w!r}")
        elif d in ("MEDIUM", "HARD"):
            if len(w) != 2:
                raise ValueError(f"{d} length invalid at #{idx}: {w!r}")
        else:
            raise ValueError(f"Unknown difficulty at #{idx}: {d!r}")
        key = (w, d)
        if key in seen:
            raise ValueError(f"Duplicate word+difficulty: {key}")
        seen.add(key)
        if not p.strip():
            raise ValueError(f"Empty pinyin at #{idx}: {w!r}")
        if re.search(r"[\u4e00-\u9fff]", h):
            raise ValueError(f"Hint contains Chinese at #{idx}: {w!r} -> {h!r}")
        counts[d] += 1

    for d in ("EASY", "MEDIUM", "HARD"):
        if counts[d] < 101:
            raise ValueError(f"{d} has only {counts[d]} entries (<101)")


def main() -> int:
    repo_dir = clone_source_repo()
    seen_global: set[str] = set()

    easy = pick_words([1, 2], {1, 2}, repo_dir, seen_global)
    medium = pick_words([3, 4], {2}, repo_dir, seen_global)
    hard = pick_words([5, 6], {2}, repo_dir, seen_global)

    for name, arr in (("EASY", easy), ("MEDIUM", medium), ("HARD", hard)):
        if len(arr) < 101:
            raise RuntimeError(f"Not enough {name} words after filtering: {len(arr)}")

    all_rows: list[dict[str, str]] = []
    for d, arr in (("EASY", easy), ("MEDIUM", medium), ("HARD", hard)):
        for x in arr:
            item = dict(x)
            item["difficulty"] = d
            all_rows.append(item)

    pinyins = generate_pinyin_by_project_code([x["word"] for x in all_rows])
    out: list[dict[str, str]] = []
    for x, py in zip(all_rows, pinyins):
        out.append(
            OrderedDict(
                [
                    ("word", x["word"]),
                    ("pinyin", py),
                    ("pos_tag", x["pos_tag"]),
                    ("difficulty", x["difficulty"]),
                    ("hint", x["hint"]),
                ]
            )
        )

    verify(out)
    OUT_JSON.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    counts = {"EASY": len(easy), "MEDIUM": len(medium), "HARD": len(hard)}
    print("Regenerated:", OUT_JSON)
    print("Counts:", counts)
    print("Source:", REPO_URL)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"Failed: {exc}", file=sys.stderr)
        raise
