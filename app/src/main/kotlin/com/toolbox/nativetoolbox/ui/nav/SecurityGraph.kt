package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 加密与隐私分类路由(9 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.securityToolsGraph(back: () -> Unit) {
    composable("tool/random") { RandomToolScreen(back) }
    composable("tool/exif") { ExifToolScreen(back) }
    composable("tool/totp") { PlaceholderToolScreen("TOTP 验证器", back) }
    composable("tool/password_vault") { PlaceholderToolScreen("密码保险箱", back) }
    composable("tool/encrypt_capsule") { PlaceholderToolScreen("加密胶囊", back) }
    composable("tool/keypair_gen") { PlaceholderToolScreen("密钥对生成", back) }
    composable("tool/image_steg") { PlaceholderToolScreen("图片隐写", back) }
    composable("tool/app_permissions") { PlaceholderToolScreen("应用权限透视", back) }
    composable("tool/private_album") { PlaceholderToolScreen("私密相册", back) }
}
