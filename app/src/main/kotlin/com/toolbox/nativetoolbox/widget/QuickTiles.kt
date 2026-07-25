package com.toolbox.nativetoolbox.widget

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.toolbox.nativetoolbox.MainActivity
import com.toolbox.nativetoolbox.R

/**
 * 快捷设置磁贴:下拉通知栏直接点,一天两次的使用频率就是日常存在感。
 *
 * 手电筒直接在磁贴里开关(不用进 App),其他磁贴跳到对应工具。
 */

/** 手电筒:直接在磁贴上开关,连 App 都不用打开 */
@RequiresApi(Build.VERSION_CODES.N)
class FlashlightTile : TileService() {

    private var on = false

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        on = !on
        runCatching {
            val cm = getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val id = cm.cameraIdList.firstOrNull { camId ->
                cm.getCameraCharacteristics(camId)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (id != null) cm.setTorchMode(id, on) else on = false
        }.onFailure { on = false }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "手电筒"
            updateTile()
        }
    }
}

/** 跳到某个工具的磁贴基类 */
@RequiresApi(Build.VERSION_CODES.N)
abstract class ToolTileService(
    private val route: String,
    private val tileLabel: String,
) : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = tileLabel
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("route", route)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
class WhiteNoiseTile : ToolTileService("tool/white_noise", "白噪音")

@RequiresApi(Build.VERSION_CODES.N)
class ScanTile : ToolTileService("tool/qr", "扫码")

@RequiresApi(Build.VERSION_CODES.N)
class BookkeepingTile : ToolTileService("tool/bookkeeping", "记一笔")
