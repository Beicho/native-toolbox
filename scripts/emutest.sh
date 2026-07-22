#!/bin/bash
export ANDROID_HOME=$HOME/android-sdk
ADB=$ANDROID_HOME/platform-tools/adb
if ! pgrep -f qemu > /dev/null; then
  nohup $ANDROID_HOME/emulator/emulator -avd test -no-window -no-audio -gpu swiftshader_indirect -no-boot-anim -no-snapshot > /tmp/emu.log 2>&1 &
  echo "EMU_LAUNCHED"
fi
for i in $(seq 1 72); do
  if $ADB shell getprop sys.boot_completed 2>/dev/null | grep -q 1; then echo BOOTED; break; fi
  sleep 5
done
if ! $ADB shell getprop sys.boot_completed 2>/dev/null | grep -q 1; then
  echo BOOT_TIMEOUT; tail -5 /tmp/emu.log; exit 1
fi
$ADB install -r /workspaces/native-toolbox/app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -1
$ADB logcat -c
$ADB shell am start -n com.toolbox.nativetoolbox/.MainActivity
sleep 8
echo "=== CRASH CHECK (home) ==="
$ADB logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: " | head -25
$ADB exec-out screencap -p > /tmp/s1_home.png
echo "=== TAP TOOL CARD (JSON, approx) ==="
$ADB shell input tap 270 700
sleep 4
$ADB logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: " | head -40
$ADB exec-out screencap -p > /tmp/s2_tool.png
ls -la /tmp/s1_home.png /tmp/s2_tool.png
