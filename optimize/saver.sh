#!/usr/bin/env bash
# Kuwait settings: show | cities | city NAME [LAT LON] | card prayers|forecast|none | home-card prayers|forecast|none
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
send() {
  "$PY" -I optimize/tv.py "am broadcast -n kw.urtv.screensaver/.SaverReceiver -a kw.urtv.screensaver.$1 ${2:-}"
}
quote() {
  case "$1" in *"'"*) echo "Apostrophes are not supported" >&2; exit 1 ;; esac
  printf "'%s'" "$1"
}
case "${1:-}" in
  show) send GET ;;
  cities) send CITIES ;;
  city)
    [ $# -eq 2 ] || [ $# -eq 4 ] || { echo "city NAME [LAT LON]"; exit 1; }
    extra="--es city $(quote "$2")"
    [ $# -eq 4 ] && extra="$extra --es lat $(quote "$3") --es lon $(quote "$4")"
    send SET "$extra" ;;
  card) send SET "--es panel $(quote "${2:?prayers|forecast|none}")" ;;
  home-card) send SET "--es home_panel $(quote "${2:?prayers|forecast|none}")" ;;
  *) echo "show | cities | city NAME [LAT LON] | card prayers|forecast|none | home-card prayers|forecast|none"; exit 1 ;;
esac
