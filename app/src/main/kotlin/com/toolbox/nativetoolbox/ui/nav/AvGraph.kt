package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 音频视频分类路由(16 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.avToolsGraph(back: () -> Unit) {
    composable("tool/recorder") { RecorderToolScreen(back) }
    composable("tool/audio_edit") { PlaceholderToolScreen("音频剪辑", back) }
    composable("tool/audio_convert") { PlaceholderToolScreen("音频格式转换", back) }
    composable("tool/ringtone_make") { PlaceholderToolScreen("铃声制作", back) }
    composable("tool/audio_extract") { PlaceholderToolScreen("音频提取", back) }
    composable("tool/voice_change") { PlaceholderToolScreen("变声与倒放", back) }
    composable("tool/ab_player") { PlaceholderToolScreen("AB 循环播放器", back) }
    composable("tool/tts") { TtsToolScreen(back) }
    composable("tool/video_compress") { PlaceholderToolScreen("视频压缩", back) }
    composable("tool/video_to_gif") { PlaceholderToolScreen("视频转 GIF", back) }
    composable("tool/video_frame") { VideoFrameToolScreen(back) }
    composable("tool/video_info") { VideoInfoToolScreen(back) }
    composable("tool/metronome") { MetronomeToolScreen(back) }
    composable("tool/tuner") { TunerToolScreen(back) }
    composable("tool/white_noise") { WhiteNoiseToolScreen(back) }
    composable("tool/earphone_test") { EarphoneTestToolScreen(back) }
}
