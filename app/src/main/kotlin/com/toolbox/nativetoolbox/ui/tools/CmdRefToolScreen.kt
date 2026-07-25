package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private class CmdEntry(val cmd: String, val desc: String)

private val gitCmds = listOf(
    CmdEntry("git status -sb", "看当前改动和所在分支，短格式"),
    CmdEntry("git switch -c 新分支", "新建并切换分支"),
    CmdEntry("git add -p", "逐块挑选要提交的改动"),
    CmdEntry("git commit --amend --no-edit", "把改动追加进上一个提交，不改说明"),
    CmdEntry("git log --oneline --graph -20", "看最近 20 条提交的分叉图"),
    CmdEntry("git diff --stat 分支A 分支B", "看两个分支差了哪些文件"),
    CmdEntry("git restore 文件", "丢弃工作区对该文件的改动"),
    CmdEntry("git restore --staged 文件", "把文件从暂存区撤回，改动保留"),
    CmdEntry("git reset --hard origin/main", "彻底丢弃本地改动，与远端一致"),
    CmdEntry("git stash push -m 说明", "临时存起当前改动"),
    CmdEntry("git stash pop", "取回最近一次暂存的改动"),
    CmdEntry("git cherry-pick 提交号", "把某个提交摘到当前分支"),
    CmdEntry("git rebase -i HEAD~3", "交互式整理最近三个提交"),
    CmdEntry("git remote -v", "看远端地址"),
    CmdEntry("git push -u origin 分支", "首次推送并建立跟踪"),
    CmdEntry("git push --force-with-lease", "强推但避免覆盖别人的提交"),
    CmdEntry("git blame -L 10,20 文件", "看某几行是谁改的"),
    CmdEntry("git clean -fd", "删掉未跟踪的文件和目录"),
    CmdEntry("git tag -a v1.0 -m 说明", "打附注标签"),
    CmdEntry("git worktree add ../目录 分支", "同时checkout多个分支到不同目录")
)

private val linuxCmds = listOf(
    CmdEntry("du -sh * | sort -h", "看当前目录各项占用，按大小排序"),
    CmdEntry("df -h", "看磁盘剩余空间"),
    CmdEntry("find . -name \"*.log\" -mtime +7", "找七天前的日志文件"),
    CmdEntry("grep -rn 关键词 目录", "递归搜索并显示行号"),
    CmdEntry("tail -f 文件 | grep 关键词", "实时跟踪日志并过滤"),
    CmdEntry("ps aux --sort=-%mem | head", "看最吃内存的进程"),
    CmdEntry("lsof -i :8080", "看哪个进程占了 8080 端口"),
    CmdEntry("ss -tulnp", "看所有监听端口"),
    CmdEntry("systemctl status 服务名", "看服务运行状态"),
    CmdEntry("journalctl -u 服务名 -n 100 --no-pager", "看服务最近 100 行日志"),
    CmdEntry("tar czf 包名.tgz 目录", "打包压缩"),
    CmdEntry("tar xzf 包名.tgz -C 目标目录", "解压到指定目录"),
    CmdEntry("rsync -avz 源 目标", "增量同步，保留属性"),
    CmdEntry("chmod 755 文件", "改权限"),
    CmdEntry("chown 用户:组 文件", "改归属"),
    CmdEntry("ln -s 源 链接名", "建软链接"),
    CmdEntry("nohup 命令 > out.log 2>&1 &", "后台跑，断开连接也不停"),
    CmdEntry("watch -n 2 命令", "每两秒重复执行并刷新"),
    CmdEntry("sed -i 's/旧/新/g' 文件", "就地批量替换"),
    CmdEntry("awk '{print $1}' 文件", "取每行第一列"),
    CmdEntry("xargs -I {} 命令 {}", "把上游输出逐个当参数执行"),
    CmdEntry("crontab -e", "编辑定时任务")
)

private val dockerCmds = listOf(
    CmdEntry("docker ps -a", "看所有容器，含已停止的"),
    CmdEntry("docker logs -f --tail 100 容器", "跟踪容器日志最后 100 行"),
    CmdEntry("docker exec -it 容器 bash", "进容器交互 shell"),
    CmdEntry("docker stats", "实时看容器资源占用"),
    CmdEntry("docker build -t 名字:标签 .", "构建镜像"),
    CmdEntry("docker images | sort -k7 -h", "看镜像并按体积排序"),
    CmdEntry("docker image prune -a", "清理没被使用的镜像"),
    CmdEntry("docker system df", "看 Docker 总共占了多少空间"),
    CmdEntry("docker cp 容器:路径 本地路径", "从容器里拷文件出来"),
    CmdEntry("docker inspect 容器 --format \"{{.State.Status}}\"", "只取容器状态字段"),
    CmdEntry("docker compose up -d", "后台启动编排里的服务"),
    CmdEntry("docker compose logs -f 服务名", "跟踪某个服务日志"),
    CmdEntry("docker compose down -v", "停止并删除容器和卷（数据会丢）"),
    CmdEntry("docker compose restart 服务名", "只重启某个服务")
)

private val adbCmds = listOf(
    CmdEntry("adb devices", "看已连接设备"),
    CmdEntry("adb install -r 包.apk", "覆盖安装"),
    CmdEntry("adb uninstall 包名", "卸载应用"),
    CmdEntry("adb logcat -s 标签", "只看指定标签的日志"),
    CmdEntry("adb logcat *:E", "只看错误级别日志"),
    CmdEntry("adb shell pm list packages | grep 关键词", "搜已安装的包名"),
    CmdEntry("adb shell am start -n 包名/.活动名", "直接启动某个界面"),
    CmdEntry("adb shell am force-stop 包名", "强制停止应用"),
    CmdEntry("adb shell pm clear 包名", "清空应用数据"),
    CmdEntry("adb shell screencap -p /sdcard/s.png", "截屏到设备"),
    CmdEntry("adb pull /sdcard/s.png .", "从设备拉文件到电脑"),
    CmdEntry("adb shell dumpsys battery", "看电池详情"),
    CmdEntry("adb shell input tap X Y", "模拟点击坐标"),
    CmdEntry("adb shell settings put global hide_error_dialogs 1", "屏蔽系统错误弹窗")
)

private val groups = listOf(
    "Git" to gitCmds,
    "Linux" to linuxCmds,
    "Docker" to dockerCmds,
    "ADB" to adbCmds
)

@Composable
fun CmdRefToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var keyword by rememberSaveable { mutableStateOf("") }

    val source = groups[tab].second
    val filtered = if (keyword.isBlank()) source else source.filter {
        it.cmd.contains(keyword.trim(), ignoreCase = true) || it.desc.contains(keyword.trim())
    }

    ToolScaffold {
        item { SectionHeader("命令速查") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = groups.map { it.first },
                        selectedIndex = tab,
                        onSelected = { tab = it }
                    )
                    IosTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = "搜命令或用途，中英文都行"
                    )
                    Text(
                        "点任意一条复制命令。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader(if (filtered.isEmpty()) "没找到" else "共 " + filtered.size + " 条") }
        if (filtered.isEmpty()) {
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            "换个关键词试试",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                }
            }
        } else {
            item {
                GroupedCard {
                    filtered.forEachIndexed { index, entry ->
                        KeyValueRow(entry.desc, entry.cmd)
                        if (index != filtered.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
