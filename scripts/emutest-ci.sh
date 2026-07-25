#!/bin/bash
# CI 模拟器全量自测(android-emulator-runner 内执行,模拟器已就绪)
# 遍历 ToolRegistry 里全部 200 个路由,任何一个闪退 → 收集后统一失败。
set -o pipefail
PKG=com.toolbox.nativetoolbox
FAILED=0
FAILED_ROUTES=""
mkdir -p screenshots

adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
adb shell am force-stop com.google.android.apps.nexuslauncher || true
sleep 2

crashcheck() {
  local c
  c=$(adb logcat -d </dev/null | grep -cE "FATAL EXCEPTION|Fatal signal" || true)
  if [ "$c" -gt 0 ]; then
    echo "!!! CRASH at $1 !!!"
    adb logcat -d </dev/null | grep -B2 -A18 -E "FATAL EXCEPTION|Fatal signal" | head -50
    FAILED=1
    FAILED_ROUTES="$FAILED_ROUTES $1"
    adb logcat -c </dev/null
    adb shell am force-stop $PKG </dev/null
    return 1
  fi
  return 0
}

# ABI 分包后取 universal 包
APK=$(ls app/build/outputs/apk/debug/*universal*.apk 2>/dev/null | head -1)
[ -z "$APK" ] && APK=$(ls app/build/outputs/apk/debug/*.apk | head -1)
echo "installing $APK"
adb install -r "$APK"
adb shell am force-stop $PKG
adb logcat -c

# 冷启动
adb shell am start -n $PKG/.MainActivity > /dev/null
sleep 12
crashcheck launch && echo OK_launch
adb exec-out screencap -p > screenshots/home.png

# 主页上下滚(动态卡片 + 全部分类都被组合一遍)
adb shell input swipe 540 1800 540 400 300; sleep 1
adb shell input swipe 540 400 540 1800 300; sleep 1
crashcheck home_scroll && echo OK_home_scroll

# 全量遍历 200 个路由
bash scripts/gen-routes.sh > /tmp/routes.txt
total=0; passed=0
while IFS= read -r route; do
  [ -z "$route" ] && continue
  total=$((total+1))
  tag=$(echo "$route" | tr '/' '_')
  # </dev/null 必须加:adb 会吞掉循环的 stdin,否则只跑第一行就结束
  adb shell am start -n $PKG/.MainActivity --es route "$route" </dev/null > /dev/null 2>&1
  sleep 2.5
  if crashcheck "$tag"; then
    passed=$((passed+1))
  fi
done < /tmp/routes.txt
echo "ROUTES: $passed/$total"

# 关键路径截屏(给人看的抽样)
for r in tool/countdown_day tool/bookkeeping tool/notes tool/weather tool/translate; do
  tag=$(echo "$r" | tr '/' '_')
  adb shell am start -n $PKG/.MainActivity --es route "$r" > /dev/null 2>&1
  sleep 3
  adb exec-out screencap -p > "screenshots/$tag.png"
done

# 分享进入
adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "hello" -n $PKG/.MainActivity > /dev/null 2>&1
sleep 4
crashcheck share_text && echo OK_share

# 预测隐私页
adb shell am start -n $PKG/.MainActivity --es route predict_settings > /dev/null 2>&1
sleep 3
crashcheck predict_settings && echo OK_predict_settings
adb exec-out screencap -p > screenshots/predict_settings.png

if [ "$FAILED" -ne 0 ]; then
  echo "EMUTEST FAILED, crashed routes:$FAILED_ROUTES"
  exit 1
fi
echo "EMUTEST ALL GREEN ($passed/$total)"
