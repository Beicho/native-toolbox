package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 网络分类路由(13 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.networkToolsGraph(back: () -> Unit) {
    composable("tool/ip_query") { IpQueryToolScreen(back) }
    composable("tool/dns_query") { DnsQueryToolScreen(back) }
    composable("tool/whois") { WhoisToolScreen(back) }
    composable("tool/ssl_cert") { SslCertToolScreen(back) }
    composable("tool/site_check") { SiteCheckToolScreen(back) }
    composable("tool/tcp_ping") { TcpPingToolScreen(back) }
    composable("tool/speed_test") { PlaceholderToolScreen("网络测速", back) }
    composable("tool/short_url") { PlaceholderToolScreen("短链生成", back) }
    composable("tool/unshort_url") { UnshortUrlToolScreen(back) }
    composable("tool/temp_mail") { TempMailToolScreen(back) }
    composable("tool/phone_share") { PlaceholderToolScreen("手机网盘", back) }
    composable("tool/file_download") { PlaceholderToolScreen("文件下载器", back) }
    composable("tool/wol") { PlaceholderToolScreen("网络唤醒", back) }
}
