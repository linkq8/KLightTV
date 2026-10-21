#!/usr/bin/env bash
#   optimize/install.sh saver      شاشة KLightTV: install + make it the screensaver
#   optimize/install.sh launcher   the home screen: install + make it the default Home
#   optimize/install.sh all        both
set -euo pipefail
cd "$(dirname "$0")/.."
PY="${PY:-optimize/venv/bin/python}"
# Local artifacts only: never download an upstream APK over this Kuwait variant.
APK_DIR="${APK_DIR:-release}"
what="${1:?usage: optimize/install.sh saver|launcher|all}"
case "$what" in saver|launcher|all) ;; *) echo "Use saver, launcher or all"; exit 1;; esac

push_install() {   # $1 apk name, $2 package
  [ -f "$APK_DIR/$1" ] || { echo "Missing $APK_DIR/$1; build or extract the Kuwait APKs first."; exit 1; }
  cp "$APK_DIR/$1" "optimize/$1"
  "$PY" -I optimize/tv.py --push-b64 "optimize/$1" "/data/local/tmp/$1"
  local out
  out=$("$PY" -I optimize/tv.py "pm install -r /data/local/tmp/$1")
  if ! grep -q Success <<< "$out"; then
    echo "Install failed; the existing app and its settings were preserved: $out"; exit 1
  fi
  "$PY" -I optimize/tv.py "rm -f /data/local/tmp/$1"
}

if [ "$what" = saver ] || [ "$what" = all ]; then
  push_install KLightTV-screensaver-android-tv.apk kw.urtv.screensaver
  printf '%s\n' \
    "settings put secure screensaver_enabled 1" \
    "settings put secure screensaver_components kw.urtv.screensaver/.ScreenDream" \
    "settings put secure screensaver_default_component kw.urtv.screensaver/.ScreenDream" \
    "am start -n kw.urtv.screensaver/.SettingsActivity" \
    | "$PY" -I optimize/tv.py --batch
  echo "Screensaver installed and selected. Its settings screen is open on the TV."
fi

if [ "$what" = launcher ] || [ "$what" = all ]; then
  # Which stock home does this TV have? Google TV sets: launcherx. Older Android TV: tvlauncher.
  if "$PY" -I optimize/tv.py "pm list packages com.google.android.apps.tv.launcherx" | grep -q launcherx; then
    STOCK=com.google.android.apps.tv.launcherx/com.google.android.apps.tv.launcherx.home.HomeActivity
  else
    STOCK=com.google.android.tvlauncher/.MainActivity
  fi
  echo "Stock home: $STOCK"

  # 1. The new home must be installed and working BEFORE anything is disabled.
  push_install KLightTV-home-android-tv.apk kw.urtv.home
  "$PY" -I optimize/tv.py "pm grant kw.urtv.home android.permission.ACCESS_FINE_LOCATION" || true

  # 2. Arm the safety switch on the TV: it restores the stock home in 300 s unless we confirm (slow ADB needs time).
  "$PY" -I optimize/tv.py --push-b64 optimize/deadman.sh /data/local/tmp/deadman.sh
  printf '%s\n' \
    "rm -f /data/local/tmp/home_ok" \
    "nohup sh /data/local/tmp/deadman.sh $STOCK 300 > /dev/null 2>&1 &" \
    | "$PY" -I optimize/tv.py --batch
  echo "Safety switch armed: the stock home comes back by itself in 5 minutes unless the new home works."

  # 3. Switch. Setup app first, then the stock home (this order avoided the black screen).
  printf '%s\n' \
    "cmd package set-home-activity --user 0 kw.urtv.home/.MainActivity" \
    "pm disable-user --user 0 com.google.android.tungsten.setupwraith" \
    "pm disable-user --user 0 ${STOCK%%/*}" \
    "cmd package set-home-activity --user 0 kw.urtv.home/.MainActivity" \
    "am force-stop com.android.tv.settings" \
    "input keyevent KEYCODE_HOME" \
    | "$PY" -I optimize/tv.py --batch
  # 4. Verify Home really opens the new launcher (a few tries: a slow TV), then disarm the switch.
  ours=no
  for try in 1 2 3; do
    sleep 6
    if "$PY" -I optimize/tv.py "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'" \
         | grep -q kw.urtv.home; then ours=yes; break; fi
    "$PY" -I optimize/tv.py "input keyevent KEYCODE_HOME" > /dev/null
  done
  if [ "$ours" = yes ]; then
    "$PY" -I optimize/tv.py "touch /data/local/tmp/home_ok"
    # the switch may have fired already if ADB was very slow: then the stock home is back, say so and stop
    if ! "$PY" -I optimize/tv.py "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN" \
         | grep -q kw.urtv.home; then
      echo "!! The safety switch put the stock home back (ADB was too slow). Nothing is broken: run the same command again."
      exit 1
    fi
    # 5. Only now: the stock home's helpers are dead weight (restore.sh brings them back).
    grep -vE '^\s*(#|$)' optimize/lists/home-replaced.txt | awk '{print "pm disable-user --user 0 " $1}' \
      | "$PY" -I optimize/tv.py --batch > /dev/null || true
    echo "Launcher is home. Press Home from inside an app to double-check."
    echo "Undo everything: optimize/reset.sh"
  else
    echo "!! Home did not open the new launcher. Leave the TV alone: the stock home returns within 5 minutes."
    echo "   Some TCL firmware pins Home to the stock launcher; on those sets keep the stock home."
    exit 1
  fi
fi
