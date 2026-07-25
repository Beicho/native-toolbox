#!/bin/bash
# 从 ToolRegistry.kt 提取全部工具路由,喂给 emutest.sh 做全量遍历
grep -oE '"tool/[a-z_0-9]+"' app/src/main/kotlin/com/toolbox/nativetoolbox/ui/home/ToolRegistry.kt \
  | tr -d '"' | sort -u
