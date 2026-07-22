#!/bin/bash
# Astro Kit 模拟器全流程自测
export ANDROID_HOME=$HOME/android-sdk
ADB=$ANDROID_HOME/platform-tools/adb

if ! pgrep -f qemu > /dev/null; then
  nohup $ANDROID_HOME/emulator/emulator -avd test -no-window -no-audio -gpu swiftshader_indirect -no-boot-anim -no-snapshot > /tmp/emu.log 2>&1 &
fi
for i in $(seq 1 72); do
  $ADB shell getprop sys.boot_completed 2>/dev/null | grep -q 1 && break
  sleep 5
done
$ADB shell getprop sys.boot_completed 2>/dev/null | grep -q 1 || { echo BOOT_TIMEOUT; exit 1; }

crashcheck() {
  local tag=$1
  local c
  c=$($ADB logcat -d | grep -cE "FATAL EXCEPTION|Fatal signal")
  if [ "$c" -gt 0 ]; then
    echo "!!! CRASH at $tag !!!"
    $ADB logcat -d | grep -B2 -A18 -E "FATAL EXCEPTION|Fatal signal" | head -40
    exit 1
  fi
  echo "OK_$tag"
}

$ADB install -r /workspaces/native-toolbox/app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -1
$ADB shell am force-stop com.toolbox.nativetoolbox
$ADB logcat -c
$ADB shell am start -n com.toolbox.nativetoolbox/.MainActivity > /dev/null
sleep 7
crashcheck "launch"
$ADB exec-out screencap -p > /tmp/t1_home.png

# 打开 JSON 工具
$ADB shell input tap 284 642
sleep 4
crashcheck "open_json"
$ADB exec-out screencap -p > /tmp/t2_json.png

# 返回
$ADB shell input keyevent 4
sleep 2
crashcheck "back"

# 打开 时间戳
$ADB shell input tap 795 642
sleep 4
crashcheck "open_timestamp"
$ADB exec-out screencap -p > /tmp/t3_timestamp.png

# 返回,点 Dock 设置
$ADB shell input keyevent 4
sleep 2
$ADB shell input tap 740 2226
sleep 3
crashcheck "settings_tab"
$ADB exec-out screencap -p > /tmp/t4_settings.png

echo ALL_PASS
ls -la /tmp/t*.png
