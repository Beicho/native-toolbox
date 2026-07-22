#!/bin/bash
# Astro Kit 模拟器诊断:冷启动 → 主线程状态 → 点击导航 → 界面语义树
export ANDROID_HOME=$HOME/android-sdk
ADB=$ANDROID_HOME/platform-tools/adb

$ADB shell am force-stop com.toolbox.nativetoolbox
$ADB logcat -c
$ADB shell am start -n com.toolbox.nativetoolbox/.MainActivity > /dev/null
sleep 8

PID=$($ADB shell pidof com.toolbox.nativetoolbox | tr -d '\r')
echo "PID=$PID"
if [ -z "$PID" ]; then
  echo "=== APP DEAD, CRASH LOG ==="
  $ADB logcat -d | grep -E "FATAL|AndroidRuntime" | head -30
  exit 1
fi

echo "=== MAIN THREAD STATE (2 samples, R=运行 S=等待 D=阻塞) ==="
$ADB shell "cat /proc/$PID/stat" | cut -d' ' -f3,14,15
sleep 2
$ADB shell "cat /proc/$PID/stat" | cut -d' ' -f3,14,15

echo "=== SCREENSHOT home ==="
$ADB exec-out screencap -p > /tmp/d1_home.png

echo "=== TAP JSON CARD (284,642) ==="
$ADB shell input tap 284 642
sleep 3
$ADB exec-out screencap -p > /tmp/d2_after_tap.png

echo "=== CRASH AFTER TAP ==="
$ADB logcat -d | grep -E "FATAL|AndroidRuntime: " | head -20

echo "=== UI DUMP(当前界面语义)==="
$ADB shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1
$ADB shell cat /sdcard/ui.xml 2>/dev/null | grep -oE 'text="[^"]{1,24}"' | sort -u | head -30

echo "=== MAIN THREAD STACK ==="
$ADB shell "kill -3 $PID" 2>/dev/null
sleep 2
$ADB shell "ls -t /data/anr/ 2>/dev/null | head -2" || true
$ADB logcat -d | grep -A30 '"main" prio' | head -35
echo DIAG_DONE
