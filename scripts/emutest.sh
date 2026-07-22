#!/bin/bash
# Astro Kit v0.3.0 模拟器全流程自测
export ANDROID_HOME=$HOME/android-sdk
ADB=$ANDROID_HOME/platform-tools/adb
PKG=com.toolbox.nativetoolbox

if ! pgrep -f qemu > /dev/null; then
  nohup $ANDROID_HOME/emulator/emulator -avd test -no-window -no-audio -gpu swiftshader_indirect -no-boot-anim -no-snapshot > /tmp/emu.log 2>&1 &
fi
for i in $(seq 1 72); do
  $ADB shell getprop sys.boot_completed 2>/dev/null | grep -q 1 && break
  sleep 5
done
$ADB shell getprop sys.boot_completed 2>/dev/null | grep -q 1 || { echo BOOT_TIMEOUT; exit 1; }

crashcheck() {
  local c
  c=$($ADB logcat -d | grep -cE "FATAL EXCEPTION|Fatal signal")
  if [ "$c" -gt 0 ]; then
    echo "!!! CRASH at $1 !!!"
    $ADB logcat -d | grep -B2 -A18 -E "FATAL EXCEPTION|Fatal signal" | head -40
    exit 1
  fi
  echo "OK_$1"
}

opentool() { # $1 route  $2 tag
  $ADB shell am start -n $PKG/.MainActivity --es route "$1" > /dev/null 2>&1
  sleep 3
  crashcheck "$2"
  $ADB exec-out screencap -p > /tmp/t_$2.png
}

$ADB install -r /workspaces/native-toolbox/app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -1
$ADB shell am force-stop $PKG
$ADB logcat -c
$ADB shell am start -n $PKG/.MainActivity > /dev/null
sleep 7
crashcheck "launch"
$ADB exec-out screencap -p > /tmp/t_home.png

# 深链遍历新工具(同时验证快捷方式路径)
opentool tool/jwt jwt
opentool tool/diff diff
opentool tool/cron cron
opentool tool/unit unit
opentool tool/datecalc datecalc
opentool tool/deviceinfo deviceinfo
opentool tool/level level
opentool tool/screentest screentest
opentool tool/banner banner
opentool tool/decider decider
opentool tool/wifiqr wifiqr
opentool tool/exif exif
opentool tool/pickcolor pickcolor
opentool tool/watermark watermark
opentool tool/gridcut gridcut
opentool tool/stitch stitch
opentool tool/filehash filehash

# 分享接入
$ADB shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "hello_share_test" -n $PKG/.MainActivity > /dev/null 2>&1
sleep 3
crashcheck "share"
$ADB exec-out screencap -p > /tmp/t_share.png

# 首页滚动到底(看新分类)
$ADB shell am start -n $PKG/.MainActivity --es route home > /dev/null 2>&1
sleep 2
$ADB shell input swipe 540 1900 540 500 400
sleep 1
$ADB shell input swipe 540 1900 540 500 400
sleep 1
$ADB shell input swipe 540 1900 540 500 400
sleep 1
$ADB exec-out screencap -p > /tmp/t_homebottom.png
crashcheck "scroll"

echo ALL_PASS
ls /tmp/t_*.png | wc -l
