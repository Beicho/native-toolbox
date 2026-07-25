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
    composable("tool/speed_test") { SpeedTestToolScreen(back) }
    composable("tool/short_url") { ShortUrlToolScreen(back) }
    composable("tool/unshort_url") { UnshortUrlToolScreen(back) }
    composable("tool/temp_mail") { TempMailToolScreen(back) }
    composable("tool/phone_share") { PhoneShareToolScreen(back) }
    composable("tool/file_download") { FileDownloadToolScreen(back) }
    composable("tool/wol") { WolToolScreen(back) }
}
