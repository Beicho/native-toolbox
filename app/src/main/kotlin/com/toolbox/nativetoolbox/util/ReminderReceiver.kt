package com.toolbox.nativetoolbox.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * 健康提醒的闹钟接收器:响铃时发通知,并给自己续下一次(比 setRepeating 在新系统上更可靠)。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getIntExtra("kind", -1)
        if (kind !in 0..2) return
        val (title, body, emoji) = when (kind) {
            0 -> Triple("该喝水了", "起来接杯水,顺便活动一下", "💧")
            1 -> Triple("该吃药了", "按时吃药,别拖", "💊")
            else -> Triple("坐太久了", "站起来走两步,拉伸一下肩颈", "🧘")
        }
        notify(context, kind, "$emoji $title", body)
        // 续下一次
        val intervalMin = intent.getLongExtra("intervalMin", 0)
        if (intervalMin > 0) schedule(context, kind, intervalMin)
    }

    companion object {
        private const val CHANNEL = "health_remind"

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "健康提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "喝水、吃药、久坐提醒"
                    }
                )
            }
        }

        private fun pending(context: Context, kind: Int, intervalMin: Long): PendingIntent =
            PendingIntent.getBroadcast(
                context, 700 + kind,
                Intent(context, ReminderReceiver::class.java)
                    .putExtra("kind", kind)
                    .putExtra("intervalMin", intervalMin),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        fun schedule(context: Context, kind: Int, intervalMin: Long) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // 非精确窗口闹钟:省电且不需要特殊权限,迟到几分钟无所谓
            am.setWindow(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMin * 60_000,
                10 * 60_000,
                pending(context, kind, intervalMin)
            )
        }

        fun cancel(context: Context, kind: Int) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pending(context, kind, 0))
        }

        fun notify(context: Context, id: Int, title: String, body: String) {
            ensureChannel(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return
            nm.notify(
                800 + id,
                NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .build()
            )
        }
    }
}
