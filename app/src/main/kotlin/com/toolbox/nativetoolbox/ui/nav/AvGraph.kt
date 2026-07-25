package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 音频视频分类路由(16 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.avToolsGraph(back: () -> Unit) {
    composable("tool/recorder") { RecorderToolScreen(back) }
    composable("tool/audio_edit") { AudioEditToolScreen(back) }
    composable("tool/audio_convert") { AudioConvertToolScreen(back) }
    composable("tool/ringtone_make") { RingtoneMakeToolScreen(back) }
    composable("tool/audio_extract") { AudioExtractToolScreen(back) }
    composable("tool/voice_change") { VoiceChangeToolScreen(back) }
    composable("tool/ab_player") { AbPlayerToolScreen(back) }
    composable("tool/tts") { TtsToolScreen(back) }
    composable("tool/video_compress") { VideoCompressToolScreen(back) }
    composable("tool/video_to_gif") { VideoToGifToolScreen(back) }
    composable("tool/video_frame") { VideoFrameToolScreen(back) }
    composable("tool/video_info") { VideoInfoToolScreen(back) }
    composable("tool/metronome") { MetronomeToolScreen(back) }
    composable("tool/tuner") { TunerToolScreen(back) }
    composable("tool/white_noise") { WhiteNoiseToolScreen(back) }
    composable("tool/earphone_test") { EarphoneTestToolScreen(back) }
}
