package com.alosir.card.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.alosir.card.MainActivity
import com.alosir.card.R
import com.alosir.card.sub.work.DailySurfacesRefresher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 一行小组件数据。
 * @param days 免息期天数；null 显示「—」（数据不足 5 组时补位不出现，取实际组数）
 */
data class WidgetRow(
    val rank: Int,
    val bankName: String,
    val days: Int?,
    val highlight: Boolean,
)

/**
 * 今日免息期小组件（ADR-0012，RemoteViews 实现——技术规范 §1.9 的 Plan B）。
 *
 * 展示当天免息期最长的 TOP 5 **账单组**。刷新时机：每日 0 点 + 每次 App 打开，
 * 由 DailySurfacesRefresher 统一驱动，保证与常驻通知同一份数据。
 * 小组件为纯展示，唯一交互是点击打开 App。
 */
class GracePeriodWidgetReceiver : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun refresher(): DailySurfacesRefresher
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // 数据由刷新器统一计算；此处仅保证系统回调（如重启后）时也能拿到渲染
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        kotlinx.coroutines.runBlocking {
            runCatching { ep.refresher().refresh() }
        }
    }

    companion object {
        /** 小组件最多展示的账单组数 */
        const val MAX_ROWS = 5

        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

        /**
         * 用给定行数据渲染全部小组件实例（DailySurfacesRefresher 调用）。
         * 数据不足 5 组时显示实际行数，不补空行（PRD P12）。
         */
        fun render(context: Context, rows: List<WidgetRow>) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, GracePeriodWidgetReceiver::class.java)
            )
            if (ids.isEmpty()) return
            val views = buildViews(context, rows)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context, rows: List<WidgetRow>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_grace_period)
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
            views.setTextViewText(R.id.widget_date, LocalDate.now().format(DATE_FMT))

            views.removeAllViews(R.id.widget_rows)
            // 双列网格：每行两格，最后一行可只有一格
            rows.take(MAX_ROWS).chunked(2).forEach { pair ->
                val row = RemoteViews(context.packageName, R.layout.widget_grace_period_row)
                pair.forEachIndexed { index, item ->
                    val cell = RemoteViews(context.packageName, R.layout.widget_grace_period_cell).apply {
                        setTextViewText(R.id.cell_bank, item.bankName)
                        setTextViewText(R.id.cell_days, item.days?.toString() ?: "—")
                    }
                    row.addView(if (index == 0) R.id.cell_left else R.id.cell_right, cell)
                }
                views.addView(R.id.widget_rows, row)
            }
            // 页脚：最长免息天数
            val maxDays = rows.mapNotNull { it.days }.maxOrNull()
            if (maxDays != null) {
                views.setTextViewText(R.id.widget_footer, "最长免息 $maxDays 天")
            }
            // 空数据：不显示假行，点击打开 App 去添加卡片（PRD P12 空状态）
            if (rows.isEmpty()) {
                views.setTextViewText(
                    R.id.widget_date,
                    context.getString(R.string.widget_empty_hint),
                )
            }

            val openIntent = PendingIntentCompat(context)
            views.setOnClickPendingIntent(R.id.widget_rows, openIntent)
            views.setOnClickPendingIntent(R.id.widget_title, openIntent)
            return views
        }

        private fun PendingIntentCompat(context: Context): android.app.PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return android.app.PendingIntent.getActivity(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }

}
}
