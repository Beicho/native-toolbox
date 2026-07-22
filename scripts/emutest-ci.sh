#!/bin/bash
# CI 模拟器自测(android-emulator-runner 内执行,模拟器已就绪)
set -e
PKG=com.toolbox.nativetoolbox
mkdir -p screenshots

# CI 模拟器很慢:隐藏系统 ANR/崩溃对话框,避免挡屏
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true

crashcheck() {
  local c
  c=$(adb logcat -d | grep -cE "FATAL EXCEPTION|Fatal signal" || true)
  if [ "$c" -gt 0 ]; then
    echo "!!! CRASH at $1 !!!"
    adb logcat -d | grep -B2 -A18 -E "FATAL EXCEPTION|Fatal signal" | head -50
    exit 1
  fi
  echo "OK_$1"
}

opentool() {
  adb shell am start -n $PKG/.MainActivity --es route "$1" > /dev/null 2>&1
  sleep 5
  crashcheck "$2"
  adb exec-out screencap -p > screenshots/$2.png
}

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop $PKG
adb logcat -c
adb shell am start -n $PKG/.MainActivity > /dev/null
sleep 12
crashcheck launch
adb exec-out screencap -p > screenshots/home.png

for pair in "tool/json json" "tool/timestamp timestamp" "tool/jwt jwt" "tool/diff diff" \
  "tool/cron cron" "tool/unit unit" "tool/datecalc datecalc" "tool/deviceinfo deviceinfo" \
  "tool/level level" "tool/screentest screentest" "tool/banner banner" "tool/decider decider" \
  "tool/wifiqr wifiqr" "tool/exif exif" "tool/pickcolor pickcolor" "tool/watermark watermark" \
  "tool/gridcut gridcut" "tool/stitch stitch" "tool/filehash filehash" "tool/hash hash" \
  "tool/qrcode qrcode" "tool/imagecompress imagecompress" "tool/encoding encoding"; do
  set -- $pair
  opentool "$1" "$2"
done

# 分享接入
adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "hello_share" -n $PKG/.MainActivity > /dev/null 2>&1
sleep 3
crashcheck share
adb exec-out screencap -p > screenshots/share.png

# 设置页(经 Dock 无法坐标稳定点击,直接检查主页滚动)
adb shell am start -n $PKG/.MainActivity --es route home > /dev/null 2>&1
sleep 2
adb shell input swipe 540 1900 540 400 400
sleep 1
adb shell input swipe 540 1900 540 400 400
sleep 1
adb exec-out screencap -p > screenshots/home_mid.png
adb shell input swipe 540 1900 540 400 400
sleep 1
adb shell input swipe 540 1900 540 400 400
sleep 1
adb exec-out screencap -p > screenshots/home_bottom.png
crashcheck scroll

echo ALL_PASS
ls screenshots/ | wc -l
