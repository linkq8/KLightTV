#!/usr/bin/env python3
"""Static checks for the whole repo: build, data files, photos, fonts, scripts, safety lists, private data.

    python3 tests/check.py          (needs: pip install fonttools brotli pillow)

Prints one line per check and exits non-zero if any failed. No network.
"""
import json
import py_compile
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from fontTools.ttLib import TTFont
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
failures = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    if not ok:
        failures.append(name)


def tracked(*globs):
    ignored = {".git", "node_modules", "build", ".gradle", "__pycache__", "venv", "release"}
    return [p for p in ROOT.rglob("*") if p.is_file() and not ignored.intersection(p.relative_to(ROOT).parts)
            and (not globs or any(p.match(g) for g in globs))]


# ---- build
before = {n: (ROOT / "dist" / n).read_bytes() for n in ("urtv.html", "tv.html") if (ROOT / "dist" / n).is_file()}
r = subprocess.run([sys.executable, "src/build.py"], cwd=ROOT, capture_output=True, text=True)
check("src/build.py runs", r.returncode == 0, r.stderr[-400:])
for page, limit in (("urtv.html", 12_000_000), ("tv.html", 300_000)):
    html = (ROOT / "dist" / page).read_text(encoding="utf-8")
    left = sorted(set(re.findall(r"__[A-Z][A-Z_]+__", html)))
    check(f"{page}: every build token replaced", not left, left)
    check(f"{page}: size under {limit // 1000} KB", len(html.encode()) < limit, len(html.encode()))
stale = [n for n, b in before.items() if (ROOT / "dist" / n).read_bytes() != b]
check("dist/ matched a fresh build (run src/build.py after editing src/)", len(before) == 2 and not stale, stale)

# ---- data files
for f in tracked("*.json"):
    try:
        json.loads(f.read_text(encoding="utf-8"))
        ok, err = True, ""
    except ValueError as e:
        ok, err = False, e
    check(f"valid JSON: {f.relative_to(ROOT)}", ok, err)

# ---- bundled photographs and the reduced runtime
bundled = json.loads((ROOT / "assets/backgrounds/backgrounds.json").read_text())
credits = (ROOT / "docs/PHOTO-CREDITS.md").read_text()
check("curated backgrounds exist with credits", len(bundled) >= 3 and all((ROOT / "assets/backgrounds" / b["file"]).is_file() and b["file"] in credits for b in bundled))
photographs = [b for b in bundled if not b.get("generated")]
check("original photographs retain 4K resolution", len(photographs) >= 3 and all(Image.open(ROOT / "assets/backgrounds" / b["file"]).size == (3840, 2160) for b in photographs))
generated = [b for b in bundled if b.get("generated")]
check("generated wallpaper resolution accurately disclosed", all(
    Image.open(ROOT / "assets/backgrounds" / b["file"]).size == (b.get("width"), b.get("height"))
    and (b["width"], b["height"]) == (3840, 2160)
    and abs(b["width"] / b["height"] - 16 / 9) < 0.01
    and str(b["width"]) in credits and str(b["height"]) in credits
    for b in generated))
page = (ROOT / "dist/tv.html").read_text()
check("runtime page below 40 KB", len(page.encode()) < 40000)
for removed in ["thesportsdb", "tvmaze", "coingecko", "finance.yahoo", "wikidata", "raw.githubusercontent", "__VERSES__", "__HADITH__"]:
    check("no runtime dependency: " + removed, removed not in page.lower())
check("Kuwait calculation and timezone", "prayerMethod: 9" in page and "Asia/Kuwait" in page)
for p in (ROOT / "android").rglob("AndroidManifest.xml"):
    if "build" not in p.parts:
        check("valid Android manifest: " + str(p.relative_to(ROOT)), ET.parse(p) is not None)

# ---- launcher config
cfg = ET.parse(ROOT / "android/launcher/src/main/res/values/config.xml").getroot()
arr = {a.get("name"): [i.text for i in a] for a in cfg.findall("string-array")}
check("launcher: no dock app is also hidden", not set(arr["dock_apps"]) & set(arr["hidden_apps"]))
check("launcher: screensaver app not hidden", "kw.urtv.screensaver" not in arr["hidden_apps"])

# ---- optimize: never disable a kept package
def pkgs(path):
    return {l.split("#")[0].strip() for l in path.read_text().splitlines() if l.split("#")[0].strip()}
keep = pkgs(ROOT / "optimize/lists/keep.txt")
for lst in sorted((ROOT / "optimize/lists").glob("*.txt")):
    if lst.name != "keep.txt":
        check(f"optimize: {lst.name} disables nothing from keep.txt", not pkgs(lst) & keep, sorted(pkgs(lst) & keep))

# ---- scripts
for f in tracked("*.py"):
    try:
        py_compile.compile(str(f), doraise=True)
        ok, err = True, ""
    except py_compile.PyCompileError as e:
        ok, err = False, e
    check(f"python compiles: {f.relative_to(ROOT)}", ok, err)
for f in tracked("*.sh"):
    r = subprocess.run(["bash", "-n", str(f)], capture_output=True, text=True)
    check(f"bash syntax: {f.relative_to(ROOT)}", r.returncode == 0, r.stderr)

# ---- nothing private or secret in the repo
texts = [f for f in tracked() if f.suffix in {".md", ".py", ".sh", ".java", ".xml", ".json", ".yml", ".html", ".txt", ".gradle"}]
ip = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
allowed_ip = {"0.0.0.0", "127.0.0.1"}
# the network check and its simulation hold only address ranges and made-up addresses, never someone's network
network_code = {ROOT / "optimize/find-tv.py", ROOT / "tests/find_tv_test.py"}
found = []
for f in texts:
    if f.name.endswith(".html") or f.parent.name == "photos" or f == Path(__file__).resolve():   # not this scan
        continue
    if f in network_code:
        s = f.read_text(encoding="utf-8", errors="ignore")
        if re.search(r"(?i)tailscale|\.ts\.net", s):
            found.append(f"{f.relative_to(ROOT)}: tailscale")
        continue
    s = f.read_text(encoding="utf-8", errors="ignore")
    found += [f"{f.relative_to(ROOT)}: {m}" for m in set(ip.findall(s)) - allowed_ip
              if all(0 <= int(x) <= 255 for x in m.split(".")) and not m.startswith(("1.", "2.", "3.", "4."))
              and not m.startswith(("192.0.2.", "198.51.100.", "203.0.113."))]   # reserved for examples
    if re.search(r"(?i)(client-id|access_key|api_key)\s*[:=]\s*['\"][A-Za-z0-9_-]{20,}", s):
        found.append(f"{f.relative_to(ROOT)}: looks like a key")
    if re.search(r"(?i)tailscale|\.ts\.net", s):
        found.append(f"{f.relative_to(ROOT)}: tailscale")
check("no private IPs, keys or network names", not found, found[:10])

print()
print(f"{len(failures)} failed" if failures else "all checks passed")
sys.exit(1 if failures else 0)
