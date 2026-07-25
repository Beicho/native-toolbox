package com.toolbox.nativetoolbox.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.toolbox.nativetoolbox.MainActivity
import com.toolbox.nativetoolbox.data.predict.PredictEngine
import com.toolbox.nativetoolbox.ui.home.toolCategories

/**
 * 「此刻」桌面小组件 —— 预测大脑最强的出口。
 *
 * 用户解锁手机就看到该用什么,**根本不用打开 App**。
 * 这是比主页更强的日常存在感,也是 Android 独占、iOS 抄不走的优势。
 */
class NowWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 小组件进程可能是冷启动,确保引擎初始化过
        PredictEngine.init(context)
        val tools = toolCategories().flatMap { it.tools }
        val byRoute = tools.associateBy { it.route }
        val onlineRoutes = tools.filter { it.requiresNetwork }.map { it.route }.toSet()

        val suggestions = if (PredictEngine.enabled) {
            PredictEngine.suggest(tools.map { it.route }, limit = 3, onlineRoutes = onlineRoutes)
        } else emptyList()

        val items = suggestions.mapNotNull { s ->
            byRoute[s.route]?.let { Triple(s.route, it.title, s.reason) }
        }

        provideContent {
            GlanceTheme {
                WidgetBody(context, items)
            }
        }
    }

    @Composable
    private fun WidgetBody(context: Context, items: List<Triple<String, String, String>>) {
        Column(
            GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(14.dp)
        ) {
            Text(
                "此刻",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(GlanceModifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    "多用几次,这里会自动放上你要用的工具",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp)
                )
            } else {
                items.forEachIndexed { index, (route, title, reason) ->
                    Row(
                        GlanceModifier
                            .fillMaxWidth()
                            .clickable(
                                actionStartActivity(
                                    Intent(context, MainActivity::class.java).apply {
                                        putExtra("route", route)
                                        putExtra("from_widget", true)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                )
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(GlanceModifier.defaultWeight()) {
                            Text(
                                title,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                reason,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    if (index != items.lastIndex) Spacer(GlanceModifier.height(2.dp))
                }
            }
        }
    }
}

class NowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowWidget()
}
