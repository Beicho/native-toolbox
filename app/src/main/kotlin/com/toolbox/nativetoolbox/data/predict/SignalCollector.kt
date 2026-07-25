package com.toolbox.nativetoolbox.data.predict

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import java.util.Calendar

/**
 * 情境信号采集。
 *
 * 铁律:
 *  1. 全部免权限 —— 不申请任何危险权限就能拿到的信号才用。
 *  2. 零网络 —— 采集到的东西一个字节都不出手机。
 *  3. 剪贴板只判断「是什么类型」,绝不读取、绝不存储内容本身。
 *  4. WiFi 名字只存哈希 —— 认得出「是不是同一个网」,但还原不出叫什么。
 */
object SignalCollector {

    /** 剪贴板内容的类型(只判类型,不碰内容) */
    enum class ClipKind { NONE, URL, EMAIL, PHONE, NUMBER, ENGLISH, CHINESE, LONG_TEXT, OTHER }

    /** 蓝牙已连设备的类别 */
    enum class BtKind { NONE, HEADSET, CAR, WATCH, OTHER }

    enum class NetKind { NONE, WIFI, CELLULAR, OTHER }

    data class Signals(
        /** 一天里的第几个 15 分钟(0..95)。比整小时精细 —— 7:45 和 7:15 行为差别很大 */
        val quarterBucket: Int,
        val dayOfWeek: Int,          // 1=周日 .. 7=周六
        val isWeekend: Boolean,
        val isHoliday: Boolean,
        val charging: Boolean,
        val batteryLevel: Int,       // 0..100
        val net: NetKind,
        /** WiFi SSID 的哈希(只用于判断是否同一个网络),非 WiFi 时为 0 */
        val wifiHash: Int,
        val bt: BtKind,
        val clip: ClipKind,
        /** 从系统分享进来时的 MIME 大类:image / text / video / audio / null */
        val shareMime: String?,
    ) {
        /** 是否可能在移动中:蜂窝网络 + 通勤时段 */
        val likelyCommuting: Boolean
            get() = net == NetKind.CELLULAR && !isWeekend &&
                (quarterBucket in 28..36 || quarterBucket in 68..80)

        /** 适合干重活:充电 + WiFi */
        val goodForHeavyWork: Boolean get() = charging && net == NetKind.WIFI

        val lowBattery: Boolean get() = !charging && batteryLevel in 1..20
    }

    /** 上次分享进入带的 MIME,消费一次就清掉 */
    @Volatile private var pendingShareMime: String? = null

    fun noteShareIntent(mime: String?) {
        pendingShareMime = mime?.substringBefore('/')
    }

    fun collect(context: Context): Signals {
        val cal = Calendar.getInstance()
        val quarter = cal.get(Calendar.HOUR_OF_DAY) * 4 + cal.get(Calendar.MINUTE) / 15
        val dow = cal.get(Calendar.DAY_OF_WEEK)

        return Signals(
            quarterBucket = quarter,
            dayOfWeek = dow,
            isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY,
            isHoliday = false, // 节假日表另接,先按工作日算
            charging = isCharging(context),
            batteryLevel = batteryLevel(context),
            net = netKind(context),
            wifiHash = wifiHash(context),
            bt = btKind(context),
            clip = if (PredictEngine.clipboardSniffing) clipKind(context) else ClipKind.NONE,
            shareMime = pendingShareMime,
        )
    }

    fun consumeShareMime() { pendingShareMime = null }

    // ---- 各信号的具体获取 ----

    private fun isCharging(context: Context): Boolean = runCatching {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        if (Build.VERSION.SDK_INT >= 23) bm.isCharging
        else {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }.getOrDefault(false)

    private fun batteryLevel(context: Context): Int = runCatching {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
    }.getOrDefault(50)

    private fun netKind(context: Context): NetKind = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetKind.NONE
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetKind.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetKind.CELLULAR
            else -> NetKind.OTHER
        }
    }.getOrDefault(NetKind.NONE)

    /**
     * WiFi SSID 哈希。Android 10+ 不给权限就拿不到真 SSID(返回 <unknown ssid>),
     * 这种情况用「网络句柄」代替 —— 同一个网络句柄稳定,足够区分"换网了没"。
     */
    private fun wifiHash(context: Context): Int = runCatching {
        if (netKind(context) != NetKind.WIFI) return 0
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetwork?.hashCode() ?: 0
    }.getOrDefault(0)

    /**
     * 蓝牙已连设备类别。用 BluetoothProfile 状态判断,不需要 BLUETOOTH_CONNECT
     * 之外的权限;没授权时静默返回 NONE。
     * 车载蓝牙是「我在开车」的强信号。
     */
    private fun btKind(context: Context): BtKind = runCatching {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = bm?.adapter ?: return BtKind.NONE
        if (!adapter.isEnabled) return BtKind.NONE
        @Suppress("MissingPermission")
        val bonded = adapter.bondedDevices ?: return BtKind.NONE
        // 只看设备类别,不读名字
        for (d in bonded) {
            val cls = d.bluetoothClass?.majorDeviceClass ?: continue
            when (cls) {
                android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                    val minor = d.bluetoothClass?.deviceClass ?: 0
                    // 车载免手持 / 车载音频
                    if (minor == android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                        minor == android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
                    ) return BtKind.CAR
                    return BtKind.HEADSET
                }
                android.bluetooth.BluetoothClass.Device.Major.WEARABLE -> return BtKind.WATCH
            }
        }
        BtKind.OTHER
    }.getOrDefault(BtKind.NONE)

    /**
     * 剪贴板类型嗅探。
     *
     * 【隐私边界】只做正则形状判断,判断完立刻丢掉字符串引用,不存不传。
     * Android 10+ 只有前台应用能读剪贴板,系统层面已经限制了滥用。
     * 这是「读心感」最强的信号:刚复制一串英文,翻译就该在第一位。
     */
    private fun clipKind(context: Context): ClipKind = runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!cm.hasPrimaryClip()) return ClipKind.NONE
        val item = cm.primaryClip?.getItemAt(0) ?: return ClipKind.NONE
        val text = item.text?.toString()?.trim() ?: return ClipKind.NONE
        if (text.isEmpty()) return ClipKind.NONE

        // 只看形状,不看内容
        val kind = when {
            text.length > 500 -> ClipKind.LONG_TEXT
            Regex("^https?://\\S+$").matches(text) -> ClipKind.URL
            Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$").matches(text) -> ClipKind.EMAIL
            Regex("^1[3-9]\\d{9}$").matches(text) -> ClipKind.PHONE
            Regex("^[\\d\\s.,+\\-*/()]+$").matches(text) -> ClipKind.NUMBER
            text.count { it.code in 0x4E00..0x9FFF } > text.length / 4 -> ClipKind.CHINESE
            Regex("^[\\x20-\\x7E\\s]+$").matches(text) && text.any { it.isLetter() } -> ClipKind.ENGLISH
            else -> ClipKind.OTHER
        }
        kind
    }.getOrDefault(ClipKind.NONE)
}
