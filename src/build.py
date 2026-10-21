#!/usr/bin/env python3
"""Build legacy browser previews; neither HTML file ships in the native Android APKs."""
import base64
import json
import shutil
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
FONTS = {"__F_LIGHT__":"Thmanyah-Light.woff2", "__F_REGULAR__":"Thmanyah-Regular.woff2",
         "__F_MEDIUM__":"Thmanyah-Medium.woff2", "__F_DISPLAY__":"ThmanyahDisplay-Bold.woff2"}
def data_uri(path, mime):
    return f"data:{mime};base64," + base64.b64encode(path.read_bytes()).decode("ascii")
def main():
    html = (ROOT / "src/template.html").read_text()
    backgrounds = json.loads((ROOT / "assets/backgrounds/backgrounds.json").read_text())
    # Keep the legacy browser preview directly usable with local assets.
    for folder in ("fonts", "backgrounds"):
        dest = ROOT / "dist" / folder
        if dest.exists(): shutil.rmtree(dest)
        dest.mkdir(parents=True)
        for asset in (ROOT / "assets" / folder).iterdir():
            if (folder == "fonts" and asset.name in FONTS.values()) or (folder == "backgrounds" and asset.suffix == ".jpg"):
                shutil.copy2(asset, dest / asset.name)
    for name, inline in (("urtv.html", True), ("tv.html", False)):
        page = html
        for token, font in FONTS.items():
            page = page.replace(token, data_uri(ROOT / "assets/fonts" / font, "font/woff2") if inline else "fonts/" + font)
        bgs = [{"u":data_uri(ROOT / "assets/backgrounds" / b["file"], "image/jpeg") if inline else "backgrounds/" + b["file"],
                "c":b["credit"], "d":b["brightness"]} for b in backgrounds]
        out = ROOT / "dist" / name
        out.parent.mkdir(exist_ok=True)
        out.write_text(page.replace("__BG_SET__", ",\n".join(json.dumps(b, ensure_ascii=False) for b in bgs)))
        print(f"{name}: {out.stat().st_size:,} bytes")
if __name__ == "__main__":
    main()
