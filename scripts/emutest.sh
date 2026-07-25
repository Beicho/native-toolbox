#!/bin/bash
# Astro Kit 模拟器全量自测:遍历全部 200 个工具路由,任何一个闪退即失败。
# 路由清单由 CI 从 ToolRegistry.kt 现场生成(scripts/gen-routes.sh),不会漏新工具。
set -o pipefail

ADB=${ADB:-adb}
PKG=com.toolbox.nativetoolbox
APK=${APK:-app/build/outputs/apk/debug/app-debug.apk}
ROUTES_FILE=${ROUTES_FILE:-/tmp/routes.txt}
FAILED=0

# ABI 分包后 debug 目录可能有多个 apk,选 universal 或第一个
if [ ! -f "$APK" ]; then
  APK=$(ls app/build/outputs/apk/debug/*universal*.apk 2>/dev/null | head -1)
  [ -z "$APK" ] && APK=$(ls app/build/outputs/apk/debug/*.apk 2>/dev/null | head -1)
fi
echo "APK: $APK"

$ADB install -r "$APK" 2>&1 | tail -1
$ADB shell settings put global hide_error_dialogs 1
$ADB shell am force-stop $PKG
$ADB logcat -c

crashcheck() {
  local c
  c=$($ADB logcat -d | grep -cE "FATAL EXCEPTION|Fatal signal")
  if [ "$c" -gt 0 ]; then
    echo "!!! CRASH at $1 !!!"
    $ADB logcat -d | grep -B2 -A20 -E "FATAL EXCEPTION|Fatal signal" | head -50
    FAILED=1
    # 记下来但继续跑,一次跑出所有崩溃点
    $ADB logcat -c
    $ADB shell am force-stop $PKG
    return 1
  fi
  return 0
}

# 冷启动
$ADB shell am start -n $PKG/.MainActivity > /dev/null
sleep 8
crashcheck "cold_launch" && echo "OK cold_launch"
$ADB exec-out screencap -p > /tmp/t_home.png

# 主页滚到底再滚回来(动态卡片 + 全部分类)
$ADB shell input swipe 540 1800 540 400 300; sleep 1
$ADB shell input swipe 540 1800 540 400 300; sleep 1
$ADB shell input swipe 540 400 540 1800 300; sleep 1
crashcheck "home_scroll" && echo "OK home_scroll"

# 全量遍历路由
total=0; passed=0
while IFS= read -r route; do
  [ -z "$route" ] && continue
  total=$((total+1))
  tag=$(echo "$route" | tr '/' '_')
  $ADB shell am start -n $PKG/.MainActivity --es route "$route" > /dev/null 2>&1
  sleep 2.2
  if crashcheck "$tag"; then
    passed=$((passed+1))
  else
    echo "FAILED_ROUTE: $route"
  fi
done < "$ROUTES_FILE"
echo "ROUTES: $passed/$total passed"

# 分享文本进入
$ADB shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "hello_share" -n $PKG/.MainActivity > /dev/null 2>&1
sleep 3
crashcheck "share_text" && echo "OK share_text"

# 设置页 + 预测隐私页
$ADB shell am start -n $PKG/.MainActivity > /dev/null 2>&1; sleep 2
$ADB shell am start -n $PKG/.MainActivity --es route predict_settings > /dev/null 2>&1
sleep 2.5
crashcheck "predict_settings" && echo "OK predict_settings"
$ADB exec-out screencap -p > /tmp/t_predict.png

if [ "$FAILED" -ne 0 ]; then
  echo "EMUTEST FAILED"
  exit 1
fi
echo "EMUTEST ALL GREEN ($passed/$total routes)"
