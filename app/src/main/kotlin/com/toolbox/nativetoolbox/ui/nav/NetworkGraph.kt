package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 网络分类路由(13 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.networkToolsGraph(back: () -> Unit) {
    composable("tool/ip_query") { PlaceholderToolScreen("IP 查询", back) }
    composable("tool/dns_query") { PlaceholderToolScreen("DNS 查询", back) }
    composable("tool/whois") { PlaceholderToolScreen("Whois", back) }
    composable("tool/ssl_cert") { PlaceholderToolScreen("SSL 证书", back) }
    composable("tool/site_check") { PlaceholderToolScreen("网站体检", back) }
    composable("tool/tcp_ping") { PlaceholderToolScreen("连通性测试", back) }
    composable("tool/speed_test") { PlaceholderToolScreen("网络测速", back) }
    composable("tool/short_url") { PlaceholderToolScreen("短链生成", back) }
    composable("tool/unshort_url") { PlaceholderToolScreen("短链还原", back) }
    composable("tool/temp_mail") { PlaceholderToolScreen("临时邮箱", back) }
    composable("tool/phone_share") { PlaceholderToolScreen("手机网盘", back) }
    composable("tool/file_download") { PlaceholderToolScreen("文件下载器", back) }
    composable("tool/wol") { PlaceholderToolScreen("网络唤醒", back) }
}
