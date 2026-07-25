package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 加密与隐私分类路由(9 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.securityToolsGraph(back: () -> Unit) {
    composable("tool/random") { RandomToolScreen(back) }
    composable("tool/exif") { ExifToolScreen(back) }
    composable("tool/totp") { TotpToolScreen(back) }
    composable("tool/password_vault") { PasswordVaultToolScreen(back) }
    composable("tool/encrypt_capsule") { EncryptCapsuleToolScreen(back) }
    composable("tool/keypair_gen") { KeypairGenToolScreen(back) }
    composable("tool/image_steg") { ImageStegToolScreen(back) }
    composable("tool/app_permissions") { AppPermissionsToolScreen(back) }
    composable("tool/private_album") { PlaceholderToolScreen("私密相册", back) }
}
